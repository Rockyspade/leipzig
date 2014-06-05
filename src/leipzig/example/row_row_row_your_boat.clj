(ns leipzig.example.row-row-row-your-boat
  (:require [overtone.live :as overtone]
            [leipzig.melody :refer :all]
            [leipzig.scale :refer :all]
            [leipzig.canon :refer :all]
            [leipzig.live :as live]))

(overtone/definst beep [freq 440]
  (-> freq
      overtone/saw
      (* (overtone/env-gen (overtone/perc) :action overtone/FREE))))

(defmethod live/play-note :leader [{midi :pitch}]
  (-> midi overtone/midi->hz beep))

(defmethod live/play-note :follower [{midi :pitch}]
  (-> midi overtone/midi->hz beep))

(defmethod live/play-note :bass [{midi :pitch}]
  (-> midi overtone/midi->hz beep))

(def melody "A simple melody built from durations and pitches."
               ; Row, row, row your boat,
  (->> (phrase [3/3 3/3 2/3 1/3 3/3]
               [  0   0   0   1   2])
    (then
               ; Gently down the stream,
       (phrase [2/3 1/3 2/3 1/3 6/3]
               [  2   1   2   3   4]))
    (then
               ; Merrily, merrily, merrily, merrily,
       (phrase (repeat 12 1/3) 
               (mapcat (partial repeat 3) [7 4 2 0])))
    (then
               ; Life is but a dream!
       (phrase [2/3 1/3 2/3 1/3 6/3] 
               [  4   3   2   1   0]))
    (where :part (is :leader))))

(def bass "A bass part to accompany the melody."
  (->> (phrase [1  1 2]
               [0 -3 0])
     (where :part (is :bass))
     (times 4)))

(defn row-row
  "Play the tune 'Row, row, row your boat' as a round."
  [speed key]
  (->> melody
    (with bass)
    (times 2)
    (canon (comp (simple 4)
                 (partial where :part (is :follower))))
    (where :time speed)
    (where :duration speed)
    (where :pitch key)
    live/play))

(comment
  (row-row (bpm 120) (comp C sharp major))
  (row-row (bpm 90) (comp low B flat minor))
)
