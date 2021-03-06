[Leipzig](https://github.com/ctford/leipzig)
============================================

[![Build Status](https://travis-ci.org/ctford/leipzig.png)](https://travis-ci.org/ctford/leipzig)

A composition library for [Overtone](https://github.com/overtone/overtone) by [@ctford](https://github.com/ctford).

Use
---
Include it as a dependency in your project.clj, along with Overtone:

    [overtone "0.9.1"]
    [leipzig "0.8.1"]

Leiningen template
------------------

There is a [Leiningen template](https://github.com/ctford/leipzig-template) that creates a simple Leipzig project,
ready to run.

Get started
-----------

Leipzig models music as a sequence of notes, each of which is a map:

    {:time 1
     :pitch 67
     :duration 2
     :part :melody}

You can create a melody with the `phrase` function. Here's a simple melody:

    (require '[leipzig.melody :refer [bpm is phrase then times where with]])

    (def melody
             ; Row,  row,  row   your  boat
      (phrase [3/3   3/3   2/3   1/3   3/3]
              [  0     0     0     1     2]))

The first argument to `phrase` is a sequence of durations. The second is a sequence of pitches. `phrase` builds a sequence of notes which we can transform with sequence functions, either from Leipzig or ones from Clojure's core libraries.

To play a melody, first define an arrangement. `play-note` is a multimethod that dispatches on the `:part` key of each note, so you can easily define an instrument responsible for playing notes of each part. Then, put the sequence of notes into a particular key and tempo and pass them along to `play`:

    (require '[overtone.live :as overtone]
             '[leipzig.live :as live]
             '[leipzig.scale :as scale])

    (overtone/definst beep [freq 440 dur 1.0]
      (-> freq
          overtone/saw
          (* (overtone/env-gen (overtone/perc 0.05 dur) :action overtone/FREE))))

    (defmethod live/play-note :default [{midi :pitch seconds :duration}]
      (-> midi overtone/midi->hz (beep seconds)))

    (->>
      melody
      (where :time (bpm 90))
      (where :duration (bpm 90))
      (where :pitch (comp scale/C scale/major))
      live/play)

There's nothing magic about `where`. It just applies a function to a particular key of each note, like `update-in` for sequences.

Let's define two other parts to go with the original melody:

    (def reply "The second bar of the melody."
             ; Gent -ly  down the stream
      (phrase [2/3  1/3  2/3  1/3  6/3]
              [  2    1    2    3    4]))

    (def bass "A bass part to accompany the melody."
      (->> (phrase [1  1 2]
                   [0 -3 0])
           (where :part (is :bass))))

    (defmethod live/play-note :bass [{midi :pitch}]
      ; Halving the frequency drops the note an octave.
      (-> midi overtone/midi->hz (/ 2) (beep 0.5)))

You can then put multiple series of notes together:

    (->>
      bass
      (then (with bass melody))
      (then (with bass melody reply))
      (then (times 2 bass))
      (where :time (bpm 90))
      (where :duration (bpm 90))
      (where :pitch (comp scale/C scale/major))
      live/play)

Advanced use
------------

In addition to simple pitches, `phrase` can take maps representing chords or `nil`s:

    (require '[leipzig.chord :as chord])

    (def chords "Off-beat chords."
      (->> (phrase (repeat 1/2)
                   [nil chord/triad
                    nil (-> chord/seventh (chord/root 4) (chord/inversion 1) (dissoc :v))
                    nil chord/triad
                    nil chord/triad])
           (where :part (is :chords))))

The maps generate a note for each value in the map - the keys are used only to enable chord-transforming functions such as `root` and `inversion`.

The `nil`s generate notes without pitches, representing rests. This is convenient, because it allows melodies to have a duration extending beyond their last audible note. However, the `play-note` implementations and `where` invocations must be prepared to handle this, e.g. by using `when` and `where`'s variation `wherever`:

    (require '[leipzig.melody :refer [wherever]]
             '[leipzig.scale :refer [lower]])

    (defmethod live/play-note :chords [{midi :pitch}]
      (when midi (-> midi overtone/midi->hz beep)))

    (->>
      (times 2 chords)
      (wherever :pitch, :pitch lower)
      (with (->> melody (then reply)))
      (where :time (bpm 90))
      (where :duration (bpm 90))
      (wherever :pitch, :pitch (comp scale/C scale/major))
      live/play)

Examples
--------

See [Row, row, row your boat](src/leipzig/example/row_row_row_your_boat.clj) or
[whelmed](https://github.com/ctford/whelmed) for examples.

In [Leipzig from scratch](https://www.youtube.com/watch?v=Lp_kQh34EWA), I demonstrate how to create a piece from
`lein new` onwards.

Leipzig came out of a talk I gave called
[Functional Composition](http://www.infoq.com/presentations/music-functional-language), where I explain basic music
theory using Overtone and Clojure.

API
---

[API documentation](http://ctford.github.io/leipzig/), generated by [Codox](https://github.com/weavejester/codox).

Design
------

Leipzig is designed to play nicely with Clojure's standard sequence functions. Therefore, Leipzig's functions for transforming notes all take the sequence as a final argument so that they can be threaded with the `->>` macro:

    (->>
      (phrase (repeat 1) (cycle [0 2 4]))
      (take 24)
      (filter #(-> % :time even?)))

These sequence functions all exhibit "closure" i.e. their result is the same shape as their input. That allows them to be used and combined very flexibly. `where` for example, can raise the pitch, set the part or put the notes into a particular tempo:

    (->> notes (where :pitch inc))
    (->> notes (where :part (is :melody)))
    (->> notes (where :time (bpm 90)))

Leipzig aims to be a library rather than a framework or environment. It uses simple Clojure datastructures and strives to be as open as possible. A new timing scheme, tuning or tempo can be mixed with Leipzig's other functions just as easily as the ones that come with the library.

Testing
-------

To run the unit tests without having to start Overtone's Supercollider server:

    lein midje leipzig.test.*

Issues
------

As pointed out by [@clojens](https://github.com/clojens), `leipzig.live` imports `overtone.live`, which implicitly boots an internal Supercollider server and [can cause problems for folks using 64 bit Windows](https://github.com/ctford/leipzig/issues/4).
