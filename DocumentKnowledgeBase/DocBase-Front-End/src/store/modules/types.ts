import { RouteRecordName } from "vue-router";
import { DictionaryData, CurrentUserInfoDTO } from "../../api/common/login";

export type cacheType = {
  mode: string;
  name?: RouteRecordName;
};

export type positionType = {
  startIndex?: number;
  length?: number;
};

export type appType = {
  sidebar: {
    opened: boolean;
    withoutAnimation: boolean;
    isClickCollapse: boolean;
  };
  layout: string;
  device: string;
};

export type multiType = {
  path: string;
  name: string;
  meta: any;
  query?: object;
  params?: object;
};

export type setType = {
  title: string;
  fixedHeader: boolean;
  hiddenSideBar: boolean;
};

export type userType = {
  username?: string;
  roles?: Array<string>;
  dictionaryList: Map<String, Array<DictionaryData>>;
  dictionaryMap: Record<string, Record<string, DictionaryData>>;
  currentUserInfo?: CurrentUserInfoDTO;
};
