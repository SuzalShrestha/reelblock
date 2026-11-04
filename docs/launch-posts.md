# Launch posts

Copy, paste, post. Replace nothing; the link is already in.
Read each community's self-promotion rules before you post. Post as yourself and answer comments.

## One-sentence pitch

Reel Block is a free, open-source Android app that closes Instagram Reels the moment they open, and
keeps everything else in Instagram.

**Tagline:** Keep Instagram. Lose the Reels.

## GitHub repo description (max 350 chars)

Free, open-source Android app that closes Instagram Reels and keeps the rest of Instagram. About 830
lines of Java (the part that acts on Instagram is 56), no root, no permissions at all.

## X / Threads / Bluesky

> I kept opening Instagram to answer one DM and losing 40 minutes to Reels.
>
> So I made an Android app that closes Reels the moment they open. DMs, feed and Stories all still
> work.
>
> Free, open source, no permissions at all, no internet.
> https://github.com/SuzalShrestha/reelblock

## Reddit (r/nosurf, r/digitalminimalism, r/androidapps, r/opensource)

**Title:** I made a free, open-source Android app that closes Instagram Reels but keeps the rest of
Instagram

> App blockers never worked for me. If I block all of Instagram, I lose my DMs and my friends'
> Stories, so I turn the blocker off after two days.
>
> Reel Block removes only the endless part. When the Reels viewer opens, it presses Back.
> You land on your feed before the first video plays.
>
> - Instagram feed, Stories and DMs all still work
> - No root, no modified Instagram, no account
> - No permissions at all, not even internet, so it cannot send data anywhere
> - About 830 lines of Java in 8 files. The part that acts on Instagram is 56 lines.
>
> It uses an accessibility service, which I know sounds scary. That is exactly why the source is
> small and public. Before Android settings open, the app shows a "What Reel Block can see" screen
> that lists what it can and never does, and you tap Agree or Not now.
>
> https://github.com/SuzalShrestha/reelblock
>
> If it breaks after an app update, open an issue. Fixes are usually one line.

## Hacker News (Show HN)

**Title:** Show HN: Reel Block – an Android app that closes Instagram Reels (service is 56 lines)

> Instagram calls Reels "clips" internally. The full-screen viewer is one view with a stable ID
> (`clips_viewer_view_pager`). Reel Block is an accessibility service that watches only the Instagram
> package, and presses Back when that view becomes visible. No root, no permissions at all, no
> Gradle: the build is a short shell script around aapt2, javac and d8. The whole app is about 830
> lines of Java; the service is 56.
>
> The README shows how I found the ID with `aapt2 dump resources` and `dumpsys activity top`, so
> anyone can fix it when the app changes.

## Message to a friend

> You said you want to stop scrolling Reels. This closes Instagram Reels and keeps the rest of
> Instagram. Free, 2-minute setup: https://github.com/SuzalShrestha/reelblock
