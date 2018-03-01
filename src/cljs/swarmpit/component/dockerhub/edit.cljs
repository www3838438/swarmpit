(ns swarmpit.component.dockerhub.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce loading? (atom false))

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :dockerhub-user {:id user-id})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

(defn- update-user-handler
  [user-id]
  (ajax/post
    (routes/path-for-backend :dockerhub-user-update {:id user-id})
    {:params     (state/get-value cursor)
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-info {:id user-id}))
                   (message/info
                     (str "User " user-id " has been updated.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User update failed. Reason: " (:error response))))}))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (user-handler id))))

(rum/defc form-edit < rum/static [user]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker
                 (:username user))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(update-user-handler (:_id user))
          :label      "Save"
          :primary    true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :dockerhub-user-info {:id (:_id user)})
          :label "Back"}))]]
   [:div.form-edit
    (form/form
      nil
      (form-public (:public user)))]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [user (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit user))))
