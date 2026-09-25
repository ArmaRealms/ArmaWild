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
The message can be customized with MiniMessage in `language-settings.yml`; see [configuration](config.md#rtp-confirmation).

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

|             | Named profile                                                    |
|-------------|------------------------------------------------------------------|
| Usage       | `/rtp <profile>`                                                 |
| Permissions | `jakesrtp.use` & `jakesrtp.usebyname`                              |
| Description | Teleports you using the selected profile.                        |

|             | Paid RTP confirmation                                            |
|-------------|------------------------------------------------------------------|
| Usage       | `/rtp confirm` or `/rtp <profile> confirm`                        |
| Permissions | `jakesrtp.use`; named profiles also require `jakesrtp.usebyname`   |
| Description | Confirms the cost and proceeds with the regular RTP checks.       |

## `/rtp-admin` - The general admin command for this plugin.

|             | Base Example                                            |
|-------------|---------------------------------------------------------|
| Usage       | `/rtp-admin <reload`&#124;`status>`                     
| Permission  | `jakesrtp.admin` defaults to: `op only`                 
| Description | All the admin functionality (of J-RTP) in its own place 
