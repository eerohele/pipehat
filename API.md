# pipehat.api 


Read and write vertical bar encoded HL7 messages (v2.x).



## `ack`
``` clojure

(ack writer)
```


Given a `java.io.BufferedWriter`, write a Commit Acknowledgement Block (ACK)
  into the writer and flush.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L150-L159)</sub>
## `nak`
``` clojure

(nak writer)
```


Given a `java.io.BufferedWriter`, write a Negative Commit Acknowledgement
  Block (NAK) into the writer and flush.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L161-L170)</sub>
## `read`
``` clojure

(read reader)
(read reader options)
```


Given a `java.io.PushbackReader`, parse the HL7 message in the reader and
  return the result.

  Options:

  `:protocol`
    Iff `:mllp`, enable MLLP mode. In MLLP mode, the parser discards every
    character preceding the MLLP start-of-block (0x0B) character.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L17-L30)</sub>
## `read+string`
``` clojure

(read+string reader)
(read+string reader options)
```


Given a `java.io.Reader`, parse the HL7 message in the reader and return a two-
  element vector where the first element is the parsed message and the second
  element is a string containing the message.

  Same options as read.

  Caller must close the reader, as with `read`.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L66-L84)</sub>
## `read-string`
``` clojure

(read-string s)
```


Given a string containing a HL7 message, parse the string and return the
  result.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L32-L38)</sub>
## `shape`
``` clojure

(shape message)
```


Given a HL7 message parsed by read, shape it into a map that's easy to reach
  into with Clojure core functions. For example:

    user=> (get-in
             (-> "MSH|^~\&|ACME" read-string shape)
             ["MSH" 0 ["MSH" 3]])
    ;;=> "ACME"

  Shaping is a lossy operation: you CAN NOT round-trip a shaped message back
  into a HL7 string.

  Shaping is therefore only useful if you need to extract data from a message.
  It is not useful if you need to update a message.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L88-L103)</sub>
## `write`
``` clojure

(write writer message)
(write writer message options)
```


Given a `java.io.BufferedWriter` and a vector representing a HL7 message
  (presumably parsed by read), write the message into the writer.

  Options:

  `:protocol`
    Iff `:mllp`, wrap message with MLLP start-of-block (0x0B) and end-of-block
    (0x1C) characters.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L107-L121)</sub>
## `write-string`
``` clojure

(write-string message)
(write-string message options)
```


Given a vector representing a HL7 message (presumably parsed by `read`),
  write the message into a string and return the string.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L128-L137)</sub>
# pipehat.const 


HL7 and MLLP constants.



## `ACK`

The ASCII acknowledgement character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L8-L8)</sub>
## `CR`

The ASCII carriage return character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L7-L7)</sub>
## `EB`

The HL7 Minimal Lower Layer Protocol (MLLP) end-of-block character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L5-L5)</sub>
## `FF`

The ASCII form feed character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L6-L6)</sub>
## `NAK`

The ASCII negative acknowledgement character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L9-L9)</sub>
## `SB`

The HL7 Minimal Lower Layer Protocol (MLLP) start-of-block character.
<br><sub>[source](null/blob/null/src/pipehat/const.clj#L4-L4)</sub>
