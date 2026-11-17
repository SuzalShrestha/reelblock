// Checks the rule lists with the real WebKit engine on macOS. Run: ./check.sh
// 1. WebKit compiles each Rules/<part>/blockerList.json.
// 2. Each selector is valid CSS for WebKit.
// 3. On a fixture page at instagram.com, each list hides its elements and no other element.
// 4. On a page that is not instagram.com, no list hides an element.
// 5. Each "block" rule stops the page load. WebKit stops it before it sends a request.
import WebKit

let fixture = """
<div><a id="nav-reels" role="link" href="/reels/">Reels</a></div>
<a id="profile-reels" href="/someone/reels/">Reels</a>
<a id="single-reel" href="/reel/ABC/">One reel</a>
<a id="nav-explore" role="link" href="/explore/">Explore</a>
<a id="nav-home" href="/">Home</a>
<a id="nav-direct" href="/direct/inbox/">Messages</a>
<div id="stories" role="menu" aria-label="Stories"><button>story</button></div>
<div id="comment-btn" role="button"><div><svg aria-label="Comment"></svg></div></div>
<div id="like-btn" role="button"><div><svg aria-label="Like"></svg></div></div>
<div id="sugg-people" class="a"><div class="b"><a href="/explore/people/">See all</a></div></div>
<article id="post-before">friend</article>
<div><div id="caught-up" class="x1miatn0">caught up</div></div>
<article id="post-after">suggested</article>
<main><a id="grid-post" href="/p/XYZ/">post</a></main>
"""
// part -> (page URL, ids that must be hidden). All other ids must stay visible.
let cases: [(part: String, url: String, hidden: Set<String>)] = [
    ("Reels", "https://www.instagram.com/", ["nav-reels", "profile-reels"]),
    ("Stories", "https://www.instagram.com/", ["stories"]),
    ("Comments", "https://www.instagram.com/", ["comment-btn"]),
    ("Explore", "https://www.instagram.com/", ["nav-explore"]),
    ("Explore", "https://www.instagram.com/explore/", ["nav-explore", "grid-post"]),
    ("Suggested", "https://www.instagram.com/", ["sugg-people"]),
]
let blocked = [("Reels", "https://www.instagram.com/reels/"), ("Reels", "https://instagram.com/reels/ABC/"),
               ("Stories", "https://www.instagram.com/stories/someone/")]

final class Loader: NSObject, WKNavigationDelegate {
    var done: CheckedContinuation<Error?, Never>?
    func webView(_ w: WKWebView, didFinish n: WKNavigation!) { done?.resume(returning: nil); done = nil }
    func webView(_ w: WKWebView, didFail n: WKNavigation!, withError e: Error) { done?.resume(returning: e); done = nil }
    func webView(_ w: WKWebView, didFailProvisionalNavigation n: WKNavigation!, withError e: Error) { done?.resume(returning: e); done = nil }
}

var failures = 0
func check(_ ok: Bool, _ what: String) {
    print(ok ? "PASS" : "FAIL", what)
    if !ok { failures += 1 }
}

let dir = URL(fileURLWithPath: CommandLine.arguments[1])
var lists: [String: WKContentRuleList] = [:]
for part in Set(cases.map(\.part)).sorted() {
    let json = try String(contentsOf: dir.appendingPathComponent("\(part)/blockerList.json"), encoding: .utf8)
    do {
        lists[part] = try await WKContentRuleListStore.default().compileContentRuleList(forIdentifier: part, encodedContentRuleList: json)
        check(true, "\(part): WebKit compiles the list")
    } catch { check(false, "\(part): WebKit compile error: \(error)") }
    let rules = try JSONSerialization.jsonObject(with: Data(json.utf8)) as! [[String: [String: Any]]]
    let probe = WKWebView()
    for rule in rules {
        let trigger = rule["trigger"]!
        let scoped = (trigger["if-domain"] as? [String]) == ["*instagram.com"] || (trigger["url-filter"] as! String).contains("instagram\\.com/")
        check(scoped, "\(part): rule is for instagram.com only")
        if let selector = rule["action"]!["selector"] as? String {
            let ok = try? await probe.callAsyncJavaScript("try { document.querySelector(s); return true } catch (e) { return false }", arguments: ["s": selector], contentWorld: .page) as? Bool
            check(ok == true, "\(part): valid selector \(selector)")
        }
    }
}

@MainActor func hiddenIDs(part: String, url: String) async throws -> Set<String> {
    let config = WKWebViewConfiguration()
    config.userContentController.add(lists[part]!)
    let view = WKWebView(frame: .zero, configuration: config)
    let loader = Loader()
    view.navigationDelegate = loader
    _ = await withCheckedContinuation { loader.done = $0; view.loadHTMLString(fixture, baseURL: URL(string: url)) }
    let ids = try await view.callAsyncJavaScript(
        "return [...document.querySelectorAll('[id]')].filter(e => getComputedStyle(e).display === 'none').map(e => e.id)",
        contentWorld: .page) as! [String]
    return Set(ids)
}
for c in cases where lists[c.part] != nil {
    let got = try await hiddenIDs(part: c.part, url: c.url)
    check(got == c.hidden, "\(c.part) at \(c.url): hidden \(got.sorted()), expected \(c.hidden.sorted())")
    let other = try await hiddenIDs(part: c.part, url: "https://example.com/explore/")
    check(other.isEmpty, "\(c.part) at example.com: hidden \(other.sorted()), expected []")
}
for (part, url) in blocked where lists[part] != nil {
    let config = WKWebViewConfiguration()
    config.userContentController.add(lists[part]!)
    let view = WKWebView(frame: .zero, configuration: config)
    let loader = Loader()
    view.navigationDelegate = loader
    let error = await withCheckedContinuation { loader.done = $0; view.load(URLRequest(url: URL(string: url)!)) } as NSError?
    check(error?.domain == "WebKitErrorDomain" && error?.code == 104, "\(part): WebKit blocks \(url) (\(error?.localizedDescription ?? "page loaded"))")
}
print(failures == 0 ? "ALL CHECKS PASSED" : "\(failures) CHECK(S) FAILED")
exit(failures == 0 ? 0 : 1)
