import SafariServices
import SwiftUI

// One Safari content blocker extension for each part. The user turns each part on or off in Settings.
// The order and the names must agree with PARTS in gen_project.py.
let parts: [(name: String, title: String, detail: String)] = [
    ("Reels", "Hide Reels", "The Reels tab and the Reels feed"),
    ("Stories", "Hide Stories", "The Stories row at the top of the feed"),
    ("Comments", "Hide Comments", "The Comment button below a post"),
    ("Explore", "Hide Explore", "The Explore tab and the Explore grid"),
    ("Suggested", "Hide suggested accounts", "The row of suggested accounts in the feed"),
]

@main
struct ReelBlockApp: App {
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

struct ContentView: View {
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var phase
    @State private var on: [String: Bool] = [:]

    // Brand primary from docs/design-system.md: #326667 (light), #9BD0D0 (dark).
    private var brand: Color {
        scheme == .dark ? Color(red: 0x9B / 255, green: 0xD0 / 255, blue: 0xD0 / 255)
                        : Color(red: 0x32 / 255, green: 0x66 / 255, blue: 0x67 / 255)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Reel Block").font(.largeTitle.bold())
                Text("Keep Instagram. Lose the Reels.").foregroundStyle(.secondary)

                Card(title: "What is hidden in Safari") {
                    ForEach(parts, id: \.name) { part in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(part.title)
                                Text(part.detail).font(.footnote).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(on[part.name] == true ? "On" : "Off")
                                .fontWeight(.semibold)
                                .foregroundStyle(on[part.name] == true ? brand : .secondary)
                        }
                        .accessibilityElement(children: .combine)
                    }
                }

                Card(title: "Turn a part on or off") {
                    Text("1. Open the Settings app.")
                    Text("2. Tap Apps, then Safari, then Extensions.")
                    Text("3. Turn on each Reel Block part that you want.")
                    Text("4. Open instagram.com in Safari.")
                    Text("Reel Block works in Safari. It cannot change the Instagram app.")
                        .font(.footnote).foregroundStyle(.secondary)
                }

                #if SCREENTIME
                ScreenTimeCard()
                #endif

                Card(title: "Your privacy") {
                    Text("Reel Block changes instagram.com only.")
                    Text("It cannot see the pages that you open. Safari applies the rules.")
                    Text("It has no network code, no analytics, no logs and no account.")
                    Text("Nothing leaves this device.")
                }
            }
            .padding()
        }
        .task(id: phase) { await refresh() }
    }

    private func refresh() async {
        for part in parts {
            let id = Bundle.main.bundleIdentifier! + "." + part.name
            on[part.name] = (try? await SFContentBlockerManager.stateOfContentBlocker(withIdentifier: id))?.isEnabled
        }
    }
}

struct Card<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.quaternary.opacity(0.5), in: RoundedRectangle(cornerRadius: 20))
    }
}
