(ns lt.objs.langs.ocaml
  (:require [lt.object :as object]
            [lt.objs.editor :as ed]
            [lt.objs.editor.pool :as pool]
            [lt.objs.command :as cmd]
            [lt.objs.clients.tcp :as tcp]
            [lt.objs.clients :as clients]
            [lt.objs.proc :as proc]
            [lt.objs.files :as files]
            [lt.objs.plugins :as plugins]
            [lt.objs.notifos :as notifos]
            [lt.objs.popup :as popup]
            [lt.objs.eval :as eval]
            [lt.objs.console :as console])
  (:require-macros [lt.macros :refer [behavior]]))

;; TODO: Figure out what this is good for.
(object/object* ::ocaml-lang
                :tags #{:ocaml.lang})

;;
;; Note:
;; When eval'ing `plugins/*plugin-dir*` while connected to the LightTable UI
;; it will return the empty string. So I've created the little hack below such
;; that I can evaluate the buffer live and still be able to find the file. This
;; makes development easier for me while being unobtrusive for the final product
;; when being installed on other peoples servers.
;;
(def echo-server-path
  (let [dir plugins/*plugin-dir*]
      (if (nil? dir)
        "'/Users/hartmann/Library/Application Support/LightTable/plugins/lt-ocaml/py-src/echo-server.py'"
        (files/join dir "py-src/echo-server.py")
    )))

(def ocaml (object/create ::ocaml-lang))

;;
;;
;;
(defn try-connect [{:keys [info]}]
  (let [path (:path info)
        client (clients/client! :ocaml.client)
        obj (object/create ::connecting-notifier client)]
    (object/add-tags client [:tcp.client])
    (proc/exec {:command "python"
                :args [echo-server-path tcp/port (clients/->id client)]
                :cwd "."
                :env {}
                :obj obj})
     client))

;;
;; Starting the echo-ing server.
;;
;; Behaviour related to the process that will be running our
;; echoing server in the background.
;;

(behavior ::on-out
          :triggers #{:proc.out}
          :reaction (fn [this data]
                      (console/log ":on-out")
                      (let [out (.toString data)]
                        (object/update! this [:buffer] str out)
                        (when (> (.indexOf out "Connected") -1)
                          (do
                            (notifos/done-working)
                            (object/merge! this {:connected true})
                            )))))

(behavior ::on-error
          :triggers #{:proc.error}
          :reaction (fn [this data]
                      (console/log ":on-error")
                      (let [out (.toString data)]
                        (console/log out)
                        (when-not (> (.indexOf (:buffer @this) "Connected") -1)
                          (object/update! this [:buffer] str data)
                          ))
                      ))

(behavior ::on-exit
          :triggers #{:proc.exit}
          :reaction (fn [this data]
                      (console/log ":on-exit")
                      (when-not (:connected @this)
                        (notifos/done-working)
                        (popup/popup! {:header "We couldn't connect."
                                       :body [:span "Looks like there was an issue trying to connect
                                              to the project. Here's what we got:" [:pre (:buffer @this)]]
                                       :buttons [{:label "close"}]})
                        (clients/rem! (:client @this)))
                      (proc/kill-all (:procs @this))
                      (object/destroy! this)))

(object/object* ::connecting-notifier
                :triggers []
                :behaviors [::on-exit ::on-error ::on-out]
                :init (fn [this client]
                        (object/merge! this {:client client :buffer ""})
                        nil))

;;
;; Behaviours related to initiating evaluation of OCaml code.
;;

(behavior ::on-eval.one
          :triggers #{:eval.one}
          :reaction (fn [editor]
                      (let [pos (ed/->cursor editor)
                            info (:info @editor)
                            info (if (ed/selection? editor)
                                   (assoc info
                                     :code (ed/selection editor)
                                     :meta {:start (-> (ed/->cursor editor "start") :line)
                                            :end (-> (ed/->cursor editor "end") :line)})
                                   (js/alert "make a selection."))]
                        (object/raise ocaml :eval! {:origin editor
                                                    :info info}))))

(behavior ::eval!
          :triggers #{:eval!}
          :reaction (fn [this event]
                      (let [{:keys [info origin]} event
                            client (-> @origin :client :default)]
                        (notifos/working "Connecting")
                        (clients/send (eval/get-client! {:command :editor.eval.ocaml
                                                         :origin origin
                                                         :info info
                                                         :create try-connect})
                                      :editor.eval.ocaml
                                      info
                                      :only
                                      origin))))
;;
;; TODO: This is triggered when? the eval! above passes try-connect along
;;       by itself.
(behavior ::connect
          :triggers #{:connect}
          :reaction (fn [this path]
                      (try-connect {:info {:path path}})))

;;
;; Showing results of evaluation OCaml code
;;
(behavior ::ocaml-result
          :triggers #{:editor.eval.ocaml.result}
          :reaction (fn [editor res]
                      (notifos/done-working)

;;                      (object/raise editor :editor.result (:result res) {:line (:end (:meta res))
;;                                                                         :start-line (-> res :meta :start)})
                    ))
