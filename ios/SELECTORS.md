# Selectors

Date of the checks: 2026-11-16.

"Live" means: read from the DOM of www.instagram.com in a browser, logged out, viewport 375x812.
A logged-out page shows the bottom navigation bar and the Reels feed. It does not show the home feed,
the Stories row, post pages or suggested posts. Thus those selectors are **NOT CONFIRMED** on the live site.
`./check.sh` proves that WebKit accepts each selector and each URL filter. It does not prove that Instagram uses them.

Each rule has `if-domain: ["*instagram.com"]`, or a URL filter that starts with the instagram.com host.

## Reels (`Rules/Reels/blockerList.json`)

| Rule | Source | Live |
|---|---|---|
| hide `a[href$="/reels/"]` | https://github.com/Lckeeee/instagram-focus/blob/619aca43df76eab5241b29005bc2d5fa1be05a32/instagram-focus-mobile-no-reels.user.js (line 90) and https://github.com/haydenmckay/haydens-instagram-onlyfriends-filter/blob/8de468641b4b06b76304d6b27412d4bf880cb6f1/haydens_instagram_onlyfriends.txt | Confirmed. Bottom bar: 1 link `a[role=link][href="/reels/"]`. Reels feed: links `/<user>/reels/`. |
| block page load `^https?://(www\.)?instagram\.com/reels/` | The path is from the live site | Confirmed path. `/reels/` goes to `/reels/<id>/`. |

Decision: a single shared link `/reel/<id>/` stays open. A friend can send it in a message, and it is one video.
To block it also, change the filter to `/reels?/`.

Limit: a content blocker stops a page load. Instagram is a single-page app. If a link inside the page
changes the URL to `/reels/` without a page load, the block rule does not see it. The hidden link prevents that tap.

## Stories (`Rules/Stories/blockerList.json`)

| Rule | Source | Live |
|---|---|---|
| hide `[role="menu"][aria-label*="Stories" i]` | https://github.com/kevirui/minimal-extension-websites/blob/7ecf21daddc860d57053f6a4cac8fcac22898e7f/src/adapters/instagram.ts (lines 286 to 288) | **NOT CONFIRMED** (needs login). English label only. |
| hide `main [role="menu"]` | Same file | **NOT CONFIRMED** (needs login). Risk: it can hide a different menu inside `main`. |
| block page load `^https?://(www\.)?instagram\.com/stories/` | Path from https://github.com/gordonajajar/deinstagram/blob/ce2fd1eca8ad5ce181ecf9109de2c84a8b3ee591/content.js (line 32) | **NOT CONFIRMED** (needs login) |

## Comments (`Rules/Comments/blockerList.json`)

| Rule | Source | Live |
|---|---|---|
| hide `[role="button"]:has(> div > svg[aria-label="Comment"])` | Live site | Confirmed on the Reels feed only: `svg[aria-label=Comment] < div < div[role=button]`, 8 matches for 8 videos. **NOT CONFIRMED** on feed posts (needs login). English label only. |

Limit: no source gave a stable selector for the list of comments on a post page. That list is not hidden.

## Explore (`Rules/Explore/blockerList.json`)

| Rule | Source | Live |
|---|---|---|
| hide `a[href="/explore/"]` | https://github.com/rborgesds/instagram-detox/blob/b9a9b6c0edd607b5cb8adbfacc390c7b6e193c98/src/utils/selectors.ts and the haydenmckay list above | Confirmed. Bottom bar: 1 link, label "Search & Explore". |
| on `/explore/` pages, hide `main a[href*="/p/"], main a[href*="/reel/"]` | https://github.com/zaj-e/hush/blob/beda81d970a23d939459ee1b2e859dc24fe33300/src/content.css | **NOT CONFIRMED**. The logged-out Explore page shows `/popular/` links, not a grid of posts. |

Side effect: on mobile web, Search and Explore are one tab. When the tab is hidden, type `instagram.com/explore/`
in the address bar to search. The grid rule applies only after a page load of `/explore/`.

## Suggested posts (`Rules/Suggested/blockerList.json`)

| Rule | Source | Live |
|---|---|---|
| hide `div[class]:has(> div[class]:first-child > a[href="/explore/people/"])` (suggested accounts) | https://github.com/AdguardTeam/AdguardFilters/blob/master/AnnoyancesFilter/Other/sections/annoyances.txt (section "Instagram.com") | **NOT CONFIRMED** (needs login) |
| hide `div.x1miatn0, div:has(div.x1miatn0) ~ article` (the "caught up" banner and all posts after it) | The haydenmckay list above. Its author verified it in October 2026. | **NOT CONFIRMED** (needs login). `x1miatn0` is a generated class name. Instagram can change it at any time. |

Limits:
- Suggested posts between the posts of friends are not hidden. The only mark on them is the text
  "Suggested for you" (see the haydenmckay list). A content blocker cannot match text.
- The haydenmckay list reports that `display: none` on feed posts can make the feed go black.
  A content blocker has only `display: none`. This rule hides only posts after the end of the feed of friends.
  If the feed goes black, turn this part off.

## Not used

- https://github.com/uBlockOrigin/uAssets (`filters/annoyances-others.txt`): it has Instagram rules for login walls only.
- https://github.com/jvarghesetech/instagram-reels-blocker: its selectors `._aa4b, ._aa4c, ._ablh` are generated class names with no record of a check.
- Fetch of the raw HTML with `curl`: the HTML has no `<a>` elements. JavaScript makes the page.

## Changes after review (2026-11-16)

- Removed the rule `div.x1miatn0, div:has(div.x1miatn0) ~ article`. `x1miatn0` is a shared generated class, so
  the rule could hide the full feed. The Suggested list now hides only the suggested accounts row.
- The Explore grid rule now applies only to a page load of `/explore/` itself, not to `/explore/tags/` or
  other paths.
