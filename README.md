# Xtra for Twitch, Ryan's fork

<img src="https://github.com/AndreyAsadchy/Xtra/blob/197ba90cac879abd2a5645393ce361847f12fa0b/app/src/main/ic_launcher-web.png" align="left" width="100"/>

A personal fork of [Xtra](https://github.com/crackededed/Xtra), the Twitch player and browser for Android. It follows upstream's stable releases and adds a few changes of my own on top, mostly about how the player feels to handle with one thumb.

</br>

## What this fork adds

**Player gestures, in the style of mobile YouTube**

- **Flick to resize.** Flick the full player down to minimize it, and flick the mini player up to expand it again. Only the speed of the flick counts, not how far you drag, and a thumb that slows down or stops before letting go doesn't count as a flick.
- **Swipe the mini player down to close it.** Drag or flick the mini player downward and it slides off screen and stops playback. While you pull, it fades and the video dims, and a small haptic tick tells you when letting go will close it. Pull back up past that line and it snaps back instead.
- **Tap or drag up to expand.** Tapping the mini player, or dragging it up, opens the full player with chat.
- The old sideways swipe to close the mini player is gone, so a sideways brush no longer closes a stream by accident.
- Palm rejection, second fingers and edge swipes are handled so a stray touch doesn't minimize or close the player.

**Lists**

- **Lists scroll past the mini player.** Every list gets extra room at the bottom while the mini player is showing, so it no longer covers the last items.

## My take

Xtra is a great app, and upstream does the hard work: keeping up with Twitch's API, the player, chat and downloads. This fork doesn't try to replace any of that. It's a place for small comfort changes I want on my own phone that may not fit upstream's plans.

The code in this fork is written with AI coding assistants (Claude Code), reviewed and tested on an emulator and on my own phone before it lands.

## Pull requests welcome, including vibe-coded ones

Upstream doesn't accept AI-generated code. **This fork does.** If you used Claude, Codex, Copilot or anything else to write a fix or feature, open a PR here. All I ask:

- Say what it changes from the user's side (what you do, what happens now).
- Check it builds and you've tried it in the app.
- Keep it focused: one feature or fix per PR.

Changes that are good fits for upstream, and written by hand, are better sent [to upstream](https://github.com/crackededed/Xtra) directly.

## Download

Get the newest build from this fork's [latest release](https://github.com/ryankhart/Xtra/releases/latest).

- It uses the same app ID and signing key as upstream's [GitHub builds](https://github.com/crackededed/Xtra/releases/tag/latest), so it installs over those and keeps your settings and logins.
- It can't install over the [F-Droid](https://f-droid.org/packages/com.github.andreyasadchy.xtra/) version, which F-Droid signs with its own key. Uninstall that first (your settings won't carry over).

## How updates work

Everything is automatic:

1. A sync job checks upstream every few hours for a new stable release (the version upstream publishes to F-Droid).
2. When there is one, it merges that version into this fork's `master`, keeping the fork's changes. If the two clash, an AI assistant resolves the conflict and the result has to compile before it's pushed; if that fails, nothing is pushed and I get asked to help.
3. Every push to `master` builds a release APK with GitHub Actions and publishes it under [Releases](https://github.com/ryankhart/Xtra/releases).

Release names look like `Xtra 2.60.10 (fork, 1a2b3c4d)`: the upstream version, then the fork commit it was built from.

## Credits and license

Xtra was created by [AndreyAsadchy](https://github.com/AndreyAsadchy/Xtra) and is maintained by [crackededed](https://github.com/crackededed/Xtra). Thanks to both, and to everyone who contributes upstream.

Like upstream, this fork is licensed under the [GNU Affero General Public License v3.0](LICENSE). The source of every build is the commit named in its release.
