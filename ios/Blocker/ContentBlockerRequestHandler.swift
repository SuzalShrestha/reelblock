import Foundation

// The same file is in each blocker extension. Each extension has its own blockerList.json.
class ContentBlockerRequestHandler: NSObject, NSExtensionRequestHandling {
    func beginRequest(with context: NSExtensionContext) {
        let item = NSExtensionItem()
        item.attachments = [NSItemProvider(contentsOf: Bundle.main.url(forResource: "blockerList", withExtension: "json"))!]
        context.completeRequest(returningItems: [item], completionHandler: nil)
    }
}
