#!/usr/bin/env python3
"""Writes ReelBlock.xcodeproj/project.pbxproj. Run: python3 gen_project.py"""
import os

PARTS = ["Reels", "Stories", "Comments", "Explore", "Suggested"]  # same names as in App/ReelBlockApp.swift
BUNDLE = "dev.local.reelblock"
n = 0
objs = []  # (id, isa, body)


def obj(isa, **kv):
    global n
    n += 1
    i = "%024X" % n
    objs.append((i, isa, kv))
    return i


def val(v):
    if isinstance(v, list):
        return "(" + ", ".join(val(x) for x in v) + ")"
    if isinstance(v, dict):
        return "{" + " ".join(f"{k} = {val(x)};" for k, x in v.items()) + "}"
    return '"' + str(v).replace("\\", "\\\\").replace('"', '\\"') + '"'


def file(path, kind):
    return obj("PBXFileReference", lastKnownFileType=kind, path=path, sourceTree="<group>")


def configs(settings):
    ids = [obj("XCBuildConfiguration", name=name, buildSettings=settings) for name in ("Debug", "Release")]
    return obj("XCConfigurationList", buildConfigurations=ids, defaultConfigurationName="Release")


def target(name, product_type, ext, settings, sources, resources=(), extra_phases=(), deps=()):
    product = obj("PBXFileReference", explicitFileType="wrapper." + ("application" if ext == "app" else "app-extension"),
                  includeInIndex=0, path=f"{name}.{ext}", sourceTree="BUILT_PRODUCTS_DIR")
    phases = [obj("PBXSourcesBuildPhase", files=[obj("PBXBuildFile", fileRef=f) for f in sources])]
    if resources:
        phases.append(obj("PBXResourcesBuildPhase", files=[obj("PBXBuildFile", fileRef=f) for f in resources]))
    common = dict(PRODUCT_NAME="$(TARGET_NAME)", SWIFT_VERSION="5.0", GENERATE_INFOPLIST_FILE="YES",
                  CURRENT_PROJECT_VERSION="1", MARKETING_VERSION="1.0", CODE_SIGN_STYLE="Automatic")
    t = obj("PBXNativeTarget", name=name, productName=name, productType=product_type, productReference=product,
            buildPhases=phases + list(extra_phases), dependencies=list(deps), buildRules=[],
            buildConfigurationList=configs({**common, **settings}))
    return t, product


swift_app = [file("App/ReelBlockApp.swift", "sourcecode.swift"), file("App/ScreenTime.swift", "sourcecode.swift")]
handler = file("Blocker/ContentBlockerRequestHandler.swift", "sourcecode.swift")
others = [file("Blocker/Info.plist", "text.plist.xml"), file("App/ScreenTime.entitlements", "text.plist.entitlements")]
project_id = "%024X" % 999999

ext_targets, ext_products, rule_files = [], [], []
for part in PARTS:
    rules = file(f"Rules/{part}/blockerList.json", "text.json")
    rule_files.append(rules)
    t, p = target(part, "com.apple.product-type.app-extension", "appex", {
        "PRODUCT_BUNDLE_IDENTIFIER": f"{BUNDLE}.{part}",
        "INFOPLIST_FILE": "Blocker/Info.plist",
        "INFOPLIST_KEY_CFBundleDisplayName": f"Reel Block: {part}",
        "SKIP_INSTALL": "YES",
        "LD_RUNPATH_SEARCH_PATHS": "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks",
    }, [handler], [rules])
    ext_targets.append(t)
    ext_products.append(p)

embed = obj("PBXCopyFilesBuildPhase", name="Embed Foundation Extensions", dstPath="", dstSubfolderSpec=13,
            files=[obj("PBXBuildFile", fileRef=p, settings={"ATTRIBUTES": ["RemoveHeadersOnCopy"]}) for p in ext_products])
deps = [obj("PBXTargetDependency", target=t, targetProxy=obj(
    "PBXContainerItemProxy", containerPortal=project_id, proxyType=1, remoteGlobalIDString=t, remoteInfo=name))
    for t, name in zip(ext_targets, PARTS)]
app, app_product = target("ReelBlock", "com.apple.product-type.application", "app", {
    "PRODUCT_BUNDLE_IDENTIFIER": BUNDLE,
    "INFOPLIST_KEY_CFBundleDisplayName": "Reel Block",
    "INFOPLIST_KEY_UILaunchScreen_Generation": "YES",
    "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations": "UIInterfaceOrientationPortrait",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight",
    "LD_RUNPATH_SEARCH_PATHS": "$(inherited) @executable_path/Frameworks",
    # Optional Screen Time part: pass REELBLOCK_FLAGS=SCREENTIME REELBLOCK_ENTITLEMENTS=App/ScreenTime.entitlements
    "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "$(inherited) $(REELBLOCK_FLAGS)",
    "CODE_SIGN_ENTITLEMENTS": "$(REELBLOCK_ENTITLEMENTS)",
}, swift_app, extra_phases=[embed], deps=deps)

products = obj("PBXGroup", name="Products", children=[app_product] + ext_products, sourceTree="<group>")
root = obj("PBXGroup", children=swift_app + [handler] + others + rule_files + [products], sourceTree="<group>")
project_configs = configs({
    "SDKROOT": "iphoneos", "IPHONEOS_DEPLOYMENT_TARGET": "16.0", "TARGETED_DEVICE_FAMILY": "1,2",
    "ALWAYS_SEARCH_USER_PATHS": "NO", "ENABLE_USER_SCRIPT_SANDBOXING": "YES",
})
objs.append((project_id, "PBXProject", dict(
    attributes={"BuildIndependentTargetsInParallel": "YES", "LastUpgradeCheck": "1600"},
    buildConfigurationList=project_configs, compatibilityVersion="Xcode 14.0", developmentRegion="en",
    hasScannedForEncodings=0, knownRegions=["en", "Base"], mainGroup=root, productRefGroup=products,
    projectDirPath="", projectRoot="", targets=[app] + ext_targets)))

out = ["// !$*UTF8*$!", "{", "archiveVersion = 1;", "classes = {};", "objectVersion = 56;", "objects = {"]
out += [f"{i} = {{isa = {isa}; " + " ".join(f"{k} = {val(v)};" for k, v in kv.items()) + "};" for i, isa, kv in objs]
out += ["};", f"rootObject = {project_id};", "}", ""]
os.makedirs(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ReelBlock.xcodeproj"), exist_ok=True)
open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ReelBlock.xcodeproj", "project.pbxproj"), "w").write("\n".join(out))
