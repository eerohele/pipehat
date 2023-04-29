# pipehat.api 


Read and write vertical bar encoded HL7 messages (v2.x).



## `ack`
``` clojure

(ack writer)
```


Given a java.io.BufferedWriter, write a Commit Acknowledgement Block (ACK)
  into the writer and flush.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L150-L159)</sub>
## `nak`
``` clojure

(nak writer)
```


Given a java.io.BufferedWriter, write a Negative Commit Acknowledgement Block
  (NAK) into the writer and flush.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L161-L170)</sub>
## `read`
``` clojure

(read reader)
(read reader options)
```


Given a java.io.PushbackReader, parse the HL7 message in the reader and
  return the result.

  Options:

    :protocol
      Iff :mllp, enable MLLP mode. In MLLP mode, the parser discards every
      character preceding the MLLP start-of-block (0x0B) character.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L17-L30)</sub>
## `read+string`
``` clojure

(read+string reader)
(read+string reader options)
```


Given a java.io.Reader, parse the HL7 message in the reader and return a two-
  element vector where the first element is the parsed message and the second
  element is a string containing the message.

  Same options as read.

  Caller must close the reader, as with read.
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


Given a java.io.BufferedWriter and a vector representing a HL7 message
  (presumably parsed by read), write the message into the writer.

  Options:

    :protocol
      Iff :mllp, wrap message with MLLP start-of-block (0x0B) and end-of-block
      (0x1C) characters.
<br><sub>[source](null/blob/null/src/pipehat/api.clj#L107-L121)</sub>
## `write-string`
``` clojure

(write-string message)
(write-string message options)
```


Given a vector representing a HL7 message (presumably parsed by read), write
  the message into a string and return the string.
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
# pipehat.impl.const 





## `EOS`

An int indicating the end of stream.
<br><sub>[source](null/blob/null/src/pipehat/impl/const.clj#L3-L3)</sub>
## `SOE`

An int indicating the start of element.
<br><sub>[source](null/blob/null/src/pipehat/impl/const.clj#L4-L4)</sub>
# pipehat.impl.defaults 





## `encoding-characters`

A map of the default, recommended HL7 encoding characters.
<br><sub>[source](null/blob/null/src/pipehat/impl/defaults.clj#L3-L9)</sub>
# pipehat.impl.reader 





## `<<`
``` clojure

(<< s)
```

<sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L10-L12)</sub>
## `read`
``` clojure

(read reader {:keys [protocol]})
```


Given a java.io.PushbackReader on a vertical bar encoded HL7 message, parse
  the message and return it.

  When in MLLP mode, returns a string when the message is an MLLP ACK/NAK
  message, else a vector.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L376-L416)</sub>
## `read-component`
``` clojure

(read-component
 {:keys [field-separator repetition-separator component-separator sub-component-separator], :as encoding-characters}
 reader)
```


Given a map of encoding characters and a reader, read a component.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L182-L210)</sub>
## `read-encoding-character`
``` clojure

(read-encoding-character reader)
```


Given a reader, read an encoding character defined in the MSH.2 HL7 field.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L295-L301)</sub>
## `read-escape-sequence`
``` clojure

(read-escape-sequence
 {:keys [field-separator repetition-separator component-separator sub-component-separator escape-character]}
 reader)
```


Given a map of encoding characters and a reader, read an escape sequence.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L59-L86)</sub>
## `read-field`
``` clojure

(read-field {:keys [field-separator], :as encoding-characters} reader)
```


Given a map of encoding characters and a reader, read a field.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L216-L231)</sub>
## `read-fields`
``` clojure

(read-fields encoding-characters reader)
```


Given a map of encoding characters and a reader, read all fields until a
  terminator (EB, EOS, or CR).
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L237-L249)</sub>
## `read-header-segment`
``` clojure

(read-header-segment reader)
```


Given a java.io.PushbackReader, parse a HL7 message header segment (MSH).

  Return a map with:

    :encoding-characters -- a map of the encoding characters specified in the header.
    :header-segment -- the header segment (a map).
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L329-L365)</sub>
## `read-header-segment-identifier`
``` clojure

(read-header-segment-identifier reader)
```


Given a map of encoding characters and a reader, read a message header
  segment (MSH) identifier.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L303-L327)</sub>
## `read-line-break-escape-sequence`
``` clojure

(read-line-break-escape-sequence reader)
```


Given a reader, read an escape sequence representing a line break (one of
  \br.\, \X0A.\, or \X0D.\).
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L37-L53)</sub>
## `read-repetition`
``` clojure

(read-repetition
 {:keys [field-separator repetition-separator component-separator sub-component-separator], :as encoding-characters}
 reader)
```


Given a map of encoding characters and a reader, read a repetition element.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L142-L158)</sub>
## `read-segment-identifier`
``` clojure

(read-segment-identifier reader)
```


Given a reader, read a segment identifier.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L251-L264)</sub>
## `read-segments`
``` clojure

(read-segments encoding-characters reader)
```


Given a map of encoding characters and a reader, read all message segments in
  the reader.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L271-L289)</sub>
## `read-string`
``` clojure

(read-string
 {:keys [field-separator repetition-separator component-separator sub-component-separator escape-character],
  :as encoding-characters}
 reader)
```


Given a map of encoding characters and a reader, read a string.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L97-L119)</sub>
## `read-sub-component`
``` clojure

(read-sub-component
 {:keys [field-separator repetition-separator component-separator sub-component-separator], :as encoding-characters}
 reader)
```


Given a map of encoding characters and a reader, read a sub-component.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L160-L180)</sub>
## `unwrap1`
``` clojure

(unwrap1 xs)
```


Given a coll, if the coll has one element, return the element, else coll.
<br><sub>[source](null/blob/null/src/pipehat/impl/reader.clj#L126-L133)</sub>
# pipehat.impl.shaper 





## `shape`
``` clojure

(shape message)
```

<sub>[source](null/blob/null/src/pipehat/impl/shaper.clj#L55-L61)</sub>
# pipehat.impl.util 





## `element-type`
``` clojure

(element-type x)
```


Given an IMeta, return its element type.
<br><sub>[source](null/blob/null/src/pipehat/impl/util.clj#L3-L6)</sub>
# pipehat.impl.writer 





## `write`
``` clojure

(write writer message {:keys [protocol]})
```


Given a java.io.BufferedWriter and a HL7-ER7 message, write the message into
  the writer.
<br><sub>[source](null/blob/null/src/pipehat/impl/writer.clj#L82-L118)</sub>
# pipehat.specs 





## `catvec`
``` clojure

(catvec & args)
```


Macro.


Like clojure.spec.alpha/cat, but expects and generates a vectors.
<br><sub>[source](null/blob/null/src/pipehat/specs.clj#L33-L38)</sub>
