(ns user)

(comment
  (require '[engraph.api :as engraph])

  (require '[engraph.git :as git])

  (engraph/transcribe #{'pipehat.api}
    :output-path "docs/index.html"
    :src-root-uri (git/guess-src-root-uri)
    :docstring-transformer `commonmark-hiccup.core/markdown->html)
  ,,,)