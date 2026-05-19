import { defineStore } from "pinia";
import { store } from "@/store";
import { userType } from "./types";
import { routerArrays } from "@/layout/types";
import { router, resetRouter } from "@/router";
import { storageLocal, storageSession } from "@pureadmin/utils";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";
import { removeToken, sessionKey } from "@/utils/auth";
import { CurrentUserInfoDTO, DictionaryData, TokenDTO } from "@/api/common/login";

const dictionaryListKey = "ag-dictionary-list";
const dictionaryMapKey = "ag-dictionary-map";

type DictionaryListMap = Map<string, Array<DictionaryData>>;
type DictionaryRecordMap = Record<string, Record<string, DictionaryData>>;

function normalizeDictionary(
  dictionary: DictionaryListMap | Record<string, Array<DictionaryData>>
): DictionaryListMap {
  if (dictionary instanceof Map) {
    return dictionary;
  }
  return new Map(Object.entries(dictionary || {}));
}

function getStoredToken() {
  return storageSession().getItem<TokenDTO>(sessionKey);
}

export const useUserStore = defineStore({
  id: "ag-user",
  state: (): userType => ({
    username: getStoredToken()?.currentUser.userInfo.username ?? "",
    roles: getStoredToken()?.currentUser.roleKey
      ? [getStoredToken()?.currentUser.roleKey]
      : [],
    dictionaryList:
      storageLocal().getItem<DictionaryListMap>(dictionaryListKey) ?? new Map(),
    dictionaryMap:
      storageLocal().getItem<DictionaryRecordMap>(dictionaryMapKey) ?? {},
    currentUserInfo: getStoredToken()?.currentUser.userInfo ?? {}
  }),
  actions: {
    SET_USERNAME(username: string) {
      this.username = username;
    },
    SET_ROLES(roles: Array<string>) {
      this.roles = roles;
    },
    SET_CURRENT_USER_INFO(userInfo: CurrentUserInfoDTO) {
      this.currentUserInfo = userInfo;
    },
    SET_DICTIONARY(dictionary: DictionaryListMap | Record<string, Array<DictionaryData>>) {
      const dictionaryMap = normalizeDictionary(dictionary);
      const dictionaryMapTmp: DictionaryRecordMap = {};

      for (const [dictType, list] of dictionaryMap.entries()) {
        dictionaryMapTmp[dictType] = list.reduce<Record<string, DictionaryData>>(
          (map, dict) => {
            map[dict.value] = dict;
            return map;
          },
          {}
        );
      }

      this.dictionaryList = dictionaryMap;
      this.dictionaryMap = dictionaryMapTmp;

      storageLocal().setItem<DictionaryListMap>(dictionaryListKey, dictionaryMap);
      storageLocal().setItem<DictionaryRecordMap>(
        dictionaryMapKey,
        dictionaryMapTmp
      );
    },
    logOut() {
      this.username = "";
      this.roles = [];
      this.currentUserInfo = {};
      removeToken();
      useMultiTagsStoreHook().handleTags("equal", [...routerArrays]);
      resetRouter();
      router.push("/login");
    }
  }
});

export function useUserStoreHook() {
  return useUserStore(store);
}
