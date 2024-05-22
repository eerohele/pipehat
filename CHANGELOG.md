# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 1.0.2 (UNRELEASED)
- Fix repetition separator parsing in multi-component fields

  Previous versions of Pipehat parsed the repetition separator (typically `~`) in fields with multiple components (typically separated by `^`). Previously, Pipehat parsed `1^2~3^4` into `["1" ["2" "3"] "4"]`. As of this version, Pipehat parses it into `[["1" "2"] ["3" "4"]]`.

## 1.0.1 (2023-06-14)
- Fix Clojars deployment

  1.0.0 was DOA due to a deployment snafu.

## 1.0.0 (2023-05-25)
- Initial release
