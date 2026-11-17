// Optional part for the Instagram app. It is not in the default build.
// Build it with REELBLOCK_FLAGS=SCREENTIME (see README.md). It needs the Family Controls entitlement.
#if SCREENTIME
import FamilyControls
import ManagedSettings
import SwiftUI

struct ScreenTimeCard: View {
    @State private var selection = FamilyActivitySelection()
    @State private var picking = false
    @State private var message = ""

    var body: some View {
        Card(title: "Instagram app (optional)") {
            Text("Screen Time can put a shield on the Instagram app. The shield blocks the full app, not only Reels.")
            Button("Select Instagram and shield it") {
                Task {
                    do {
                        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
                        picking = true
                    } catch {
                        message = "Screen Time did not give permission."
                    }
                }
            }
            Button("Remove the shield") {
                ManagedSettingsStore().shield.applications = nil
                message = "The shield is off."
            }
            if !message.isEmpty { Text(message).font(.footnote).foregroundStyle(.secondary) }
        }
        .familyActivityPicker(isPresented: $picking, selection: $selection)
        .onChange(of: selection) { new in
            ManagedSettingsStore().shield.applications = new.applicationTokens
            message = new.applicationTokens.isEmpty ? "The shield is off." : "The shield is on."
        }
    }
}
#endif
