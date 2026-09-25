# [J-RTP] Documentation ~ _config.yml_

###### The even more in depth guide to configuring J-RTP

Go to the bottom of this page for the simplified config.

---

### `rtp-confirmation`

RTP commands with an active economy provider and a positive profile `cost` require an explicit confirmation.
Free RTP commands continue normally. No warmup, cooldown or charge starts when showing the prompt.

```yaml
rtp-confirmation:
  subcommand: 'confirm'
```

Players use `/rtp confirm` or `/rtp <profile> confirm`. Aliases such as `/wild` also work.
The subcommand is case-insensitive and must contain only letters, numbers, `_` or `-`.
Invalid values fall back to `confirm` with a console warning. Choose a value different from your profile names,
because this word is reserved for confirmation. Missing settings default to `confirm`, including existing installations.
Confirmation is explicit in the command; it does not require a previous pending request.
Permissions, current profile, balance, cooldown and warmup checks are evaluated again when confirming.

Override `confirm-paid-rtp` in `language-settings.yml` to customize the prompt using MiniMessage:

```yaml
confirm-paid-rtp: '<yellow>Este RTP custa <cost>. <confirm><green><hover:show_text:"<command>">[Clique para confirmar]</hover></green></confirm></yellow>'
```

- `<cost>`: cost formatted by the economy provider.
- `<profile>`: selected RTP profile.
- `<command>`: full confirmation command, including the alias and optional profile.
- `<confirm>...</confirm>`: makes the enclosed text run the confirmation command when clicked.

Standard MiniMessage tags, including `<click:run_command:...>` and `<hover:show_text:...>`, are supported.
All configurable messages use MiniMessage colors and named placeholders; see [message formatting](#message-formatting).
Apply configuration changes with `/rtp-admin reload` and message changes with `/rtp-admin reload-messages`.

---

### Message formatting

Every message in `language-settings.yml` and the translations supports MiniMessage, including colors,
gradients, click and hover events. Permission denials also use `no-permission` from this file.
Values such as player names, profile names and economy text are inserted literally and cannot inject tags.
Nested configured messages, such as the cooldown duration or an error reason, keep their formatting.

```yaml
need-to-wait-for-cooldown: '<yellow>Aguarde <time> para usar o RTP novamente.</yellow>'
teleporting-in-x-seconds: '<green>Teleportando em <seconds> segundos...</green>'
not-enough-money: '<red>Você precisa de <cost>. Saldo: <balance>.</red>'
you-were-charged-x: '<green>Foram cobrados <amount>. Saldo: <balance>.</green>'
no-permission: '<red>Você não tem a permissão <permission>.</red>'
cooldown-over: '<green>O RTP de <profile> está disponível!</green>'
readable-time: '<days><hours><minutes><seconds>'
readable-time-word-seconds: '<aqua><amount> segundos</aqua>'
```

Use the placeholders assigned to each message:

| Message key                                                                                                       | Placeholders                                                 |
|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| `not-permitted-generic`, `not-permitted-major-error`                                                              | `<reason>`                                                   |
| `not-enabled-in-this-world`                                                                                       | `<code>`                                                     |
| `no-settings-found-with-name`, `no-settings-found-with-name-for-player`, `cooldown-over`                          | `<profile>`                                                  |
| `no-permission`                                                                                                   | `<permission>`                                               |
| `player-not-found`                                                                                                | `<player>`                                                   |
| `world-not-found`                                                                                                 | `<world>`                                                    |
| `rtp-settings-no-contain-world`, `rtp-settings-must-use-world`                                                    | `<profile>`, `<world>`                                       |
| `need-to-wait-for-cooldown`                                                                                       | `<time>`                                                     |
| `teleporting-in-x-seconds`                                                                                        | `<seconds>`                                                  |
| `not-enough-money`                                                                                                | `<cost>`, `<balance>`                                        |
| `confirm-paid-rtp`                                                                                                | `<cost>`, `<profile>`, `<command>`, `<confirm>...</confirm>` |
| `you-were-charged-x`                                                                                              | `<amount>`, `<balance>`                                      |
| `economy-error`                                                                                                   | `<error>`                                                    |
| `settings-not-found`                                                                                              | `<profile>`, `<profiles>`                                    |
| `readable-time`                                                                                                   | `<days>`, `<hours>`, `<minutes>`, `<seconds>`                |
| `readable-time-word-days`, `readable-time-word-hours`, `readable-time-word-minutes`, `readable-time-word-seconds` | `<amount>`                                                   |

The remaining message keys do not need placeholders. `reloaded` controls the configuration reload acknowledgement.
Use `<newline>` for line breaks; written `\n` is also accepted.

**Existing installations:** update custom overrides manually. Replace numbered placeholders such as `{0}`
with the corresponding names above, `&c` with `<red>`, and `{#00FFC8}` with `<#00FFC8>`.
Legacy formatting and numbered placeholders are no longer interpreted. Existing files are not overwritten.
Reload messages with `/rtp-admin reload-messages` after editing them.

---

### `config-version`

This is an internal setting, and it should not be touched.
As changed are made to the config, the config version gets increased (by me).
Read more in the section titled `run-old-configs`.

---

### `run-old-configs`

**Default value:** `false`  
**Quick description:**
Should the plugin try to load a config with a different version?

If there is a version mismatch between the plugin's internal config and the given config,
should we attempt to load it anyway, or send warning messages in console and shut the plugin down.

Each version of the plugin expects a specific version of the config,
and using the wrong config may result in an exception getting thrown,
or default values getting silently loaded.
While the latter option is both more likely and not necessarily bad,
it could result in unexpected behavior if there have been changes to the config's structure.
If the specifics of the config do not matter much to you, it is defiantly easier to set this to true.
If you want to always know that this plugin will do what you say, e
exactly what you say, and nothing more than what you say,
set this to false so that in the case there is a mismatch, you will have to go in and fix it,
and then of course you can read the new settings and decide if you want to change them or not.

---

### `rtp-on-first-join` _<sub>...and all sub values</sub>_

**Quick description:**
If this is enabled, all new players will spawn in a random location chosen
as if they ran `/rtp` while in the world \[world], making sure to use the settings \[default]. _<sub>Note: The values
\[default] and \[world] are fillers.
They do work with the stock config, but you can change them to any valid world or rtp-settings._</sub>

```yaml
rtp-on-first-join:
  enabled:  false
  settings: default
  world:    world
```

Enabled:
Should we spawn players randomly?

Settings:
The plugin needs to know _how_ to find the random point, and it uses a rtp-config-section (also commonly referred to as
rtp-settings in this documentation) to define how to get the location.
Each rtp-config-section that you define requires a unique name, and that name is what you put here after settings
in place of \[default]. _<sub>Note: If you do not change the name of the stock rtp-config-section, default is a fully
valid name here.</sub>_

World:
Again, the plugin needs to know how to find the random point, and since rtp-settings allow multiple worlds,
you must say which world to spawn the player in. _<sub>Note: the world name IS case-sensitive, and it MUST be listed as
an enabled world in the rtp-settings</sub>_

---

# Simplified Config

Because the plugin loads its default in place of missing values in the config, you can remove all the settings you do
not plan on using.
As an example, this config-section (named simple-config) has the majority of the settings removed, leaving only the most
basic options visible.
If you do not plan on making a crazy config, it may be simpler to use this as your template instead of the one in the
config.yml.

```yaml
random-teleport-settings simple-config:
  enabled: true
  command-enabled: true
  require-explicit-permission: false
  priority: 1
  enabled-worlds:
    - world
  shape:
    value: a     # < a:square | b:circle >
  defining-points:
    radius-center:
      radius:
        max: 2000
        min: 1000
      center:
        value: a # < a:spawn | b:player-location | c:preset-values >
        x: 0     # Used for preset value only.
        z: 0     # Used for preset value only.
  cooldown:      # Cooldown between uses. Can be bypassed with permission node jakesrtp.nocooldown
    seconds: 30  # Default: 30
```

If you need to add anything later on,
either go to the [config.yml](src/main/resources/config.yml) for a quick look at all the settings
or keep reading here for all the settings _and_ a detailed description.
