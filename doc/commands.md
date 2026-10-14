# [J-RTP] Documentation ~ Commands

###### The guide to J-RTP commands: What they are, and how to use them.

## `/rtp` - The general random teleport command.

**General**  
It teleports you to a random location.   
Usage:      `/rtp`  
Aliases:    `/wild`  
Permission: `jakesrtp.use` defaults to: `all`

**Extended usages...**

`/rtp <profile>` requires the extra permission `jakesrtp.usebyname`.
This selects an RTP settings profile by name.

Paid RTPs show a clickable confirmation prompt before starting. Confirm with `/rtp confirm`
or `/rtp <profile> confirm`. The word `confirm` is configurable through `rtp-confirmation.subcommand`
in `config.yml`. `/wild` supports the same syntax. Confirmation alone does not require `jakesrtp.usebyname`.
The message can be customized with MiniMessage in `language-settings.yml`;
see [configuration](config.md#rtp-confirmation).

## `/rtp-admin` - The general admin command for this plugin.

**General**  
All the admin functionality (of J-RTP) in its own place  
Usage:      `/rtp-admin <reload|status>`  
Permission: `jakesrtp.admin` defaults to: `op`

# [J-RTP] Documentation ~ Commands (Format option two)

###### The guide to J-RTP commands: What they are, and how to use them.

## `/rtp` - The general random teleport command.

|              | Base Example                        |
|--------------|-------------------------------------|
| Usage:       | `/rtp` &nbsp;&#124;&nbsp; `/wild`   
| Permission:  | `jakesrtp.use` defaults to: `all`   
| Description: | Teleports you to a random location. 

|             | Named profile                             |
|-------------|-------------------------------------------|
| Usage       | `/rtp <profile>`                          |
| Permissions | `jakesrtp.use` & `jakesrtp.usebyname`     |
| Description | Teleports you using the selected profile. |

|             | Paid RTP confirmation                                            |
|-------------|------------------------------------------------------------------|
| Usage       | `/rtp confirm` or `/rtp <profile> confirm`                       |
| Permissions | `jakesrtp.use`; named profiles also require `jakesrtp.usebyname` |
| Description | Confirms the cost and proceeds with the regular RTP checks.      |

## `/rtp-admin` - The general admin command for this plugin.

|             | Base Example                                            |
|-------------|---------------------------------------------------------|
| Usage       | `/rtp-admin <reload`&#124;`status>`                     
| Permission  | `jakesrtp.admin` defaults to: `op only`                 
| Description | All the admin functionality (of J-RTP) in its own place 


## Paper Brigadier commands and profile shortcuts

All commands are registered using Paper's Brigadier command lifecycle. Base commands:
`/rtp` (alias `/wild`), `/forcertp <player> -c <profile>`,
`/forcertp <player> -w <world>`, and `/rtp-admin reload|status|reload-messages`.
Tab completion suggests accessible profiles, online players, and world names.

Each enabled, command-enabled file in `plugins/JakesRTP/rtpSettings/` gets a shortcut
named after the file (without `.yml`). With `end.yml`, `nether.yml`, `recursos.yml`,
`mundo.yml` and `legado.yml`, the shortcuts are
`/end`, `/nether`, `/recursos`, `/mundo` and `/legado`.
The full `/rtp end` syntax continues to require `jakesrtp.usebyname`.

Shortcuts require `jakesrtp.use` and respect `command-enabled` and
`require-explicit-permission` for each profile. They do not require
`jakesrtp.usebyname`. Paid shortcuts prompt with `/end confirm` (or the
configured confirmation literal), preserving cooldown, warmup and cost checks.

Profile names must match `[a-zA-Z0-9_-]+` and may not use reserved command
names or the confirmation literal. The plugin skips root aliases if another
command already occupies the label at registration time; review collisions with
plugins that load later.
The shortcut set and confirmation literal are registered on Paper's command
lifecycle events. A plugin-level `/rtp-admin reload` reloads RTP settings,
but adding/removing/renaming profiles requires a server restart to refresh
the root commands.
