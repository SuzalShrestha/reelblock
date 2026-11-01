# Launch posts

Copy, paste, post. Replace nothing; the link is already in.
Read each community's self-promotion rules before you post. Post as yourself and answer comments.

## One-sentence pitch

Reel Block is a free, open-source Android app that closes Instagram Reels and YouTube Shorts the
moment they open, and keeps everything else in those apps.

**Tagline:** Keep Instagram and YouTube. Lose the Reels and Shorts.

## GitHub repo description (max 350 chars)

Free, open-source Android app that closes Instagram Reels and YouTube Shorts and keeps the rest of
the apps. About 120 lines of Java, no root, no internet permission.

## X / Threads / Bluesky

> I kept opening Instagram to answer one DM and losing 40 minutes to Reels.
>
> So I made an Android app that closes Reels and YouTube Shorts the moment they open. DMs, feed,
> Stories, normal YouTube all still work.
>
> Free, open source, ~120 lines, no internet permission.
> https://github.com/SuzalShrestha/reelblock

## Reddit (r/nosurf, r/digitalminimalism, r/androidapps, r/opensource)

**Title:** I made a free, open-source Android app that closes Instagram Reels and YouTube Shorts but
keeps the rest of the apps

> App blockers never worked for me. If I block all of Instagram, I lose my DMs and my friends'
> Stories, so I turn the blocker off after two days.
>
> Reel Block removes only the endless part. When the Reels or Shorts viewer opens, it presses Back.
> You land on your feed before the first video plays.
>
> - Instagram feed, Stories, DMs, and normal YouTube videos all still work
> - No root, no modified Instagram, no account
> - No internet permission, so it cannot send data anywhere
> - About 120 lines of Java. You can read all of it in five minutes.
>
> It uses an accessibility service, which I know sounds scary. That is exactly why the source is
> small and public.
>
> https://github.com/SuzalShrestha/reelblock
>
> If it breaks after an app update, open an issue. Fixes are usually one line.

## Hacker News (Show HN)

**Title:** Show HN: Reel Block – a 120-line Android app that closes Reels and Shorts

> Instagram calls Reels "clips" internally, and YouTube calls Shorts "reels". Each app shows the
> full-screen viewer in one view with a stable ID (`clips_viewer_view_pager`, `reel_recycler`).
> Reel Block is an accessibility service that watches only those two packages, and presses Back
> when that view becomes visible. No root, no network permission, no Gradle: the build is a 20-line
> shell script around aapt2, javac and d8.
>
> The README shows how I found the IDs with `aapt2 dump resources` and `dumpsys activity top`, so
> anyone can fix them when the apps change.

## Message to a friend

> You said you want to stop scrolling Reels. This closes Reels and YouTube Shorts and keeps the rest
> of the apps. Free, 2-minute setup: https://github.com/SuzalShrestha/reelblock
