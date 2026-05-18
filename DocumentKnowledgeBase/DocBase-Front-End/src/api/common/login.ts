import { http } from "@/utils/http";
import { RouteRecordRaw } from "vue-router";

export type CaptchaDTO = {
  captchaCodeImg: string;
  captchaCodeKey: string;
};

export type ConfigDTO = {
  isCaptchaOn: boolean;
  isRegisterEnabled: boolean;
  dictionary: Map<string, Array<DictionaryData>>;
};

export type LoginByPasswordDTO = {
  username: string;
  password: string;
  captchaCode: string;
  captchaCodeKey: string;
};

export type RegisterDTO = {
  username: string;
  nickname?: string;
  email?: string;
  phoneNumber?: string;
  password: string;
  status?: number;
  remark?: string;
};

export type TokenDTO = {
  token: string;
  currentUser: CurrentLoginUserDTO;
};

export type CurrentLoginUserDTO = {
  userInfo: CurrentUserInfoDTO;
  roleKey: string;
  permissions: Set<string>;
};

export interface CurrentUserInfoDTO {
  avatar?: string;
  createTime?: Date;
  creatorId?: number;
  creatorName?: string;
  deptId?: number;
  deptName?: string;
  email?: string;
  loginDate?: Date;
  loginIp?: string;
  nickName?: string;
  phoneNumber?: string;
  postId?: number;
  postName?: string;
  remark?: string;
  roleId?: number;
  roleName?: string;
  sex?: number;
  status?: number;
  updaterId?: number;
  updaterName?: string;
  updateTime?: Date;
  userId?: number;
  username?: string;
  userType?: number;
}

export type DictionaryData = {
  label: string;
  value: number;
  cssTag: string;
};

export const getConfig = () => {
  return http.request<ResponseData<ConfigDTO>>("get", "/getConfig");
};

export const getCaptchaCode = () => {
  return http.request<ResponseData<CaptchaDTO>>("get", "/captchaImage");
};

export const loginByPassword = (data: LoginByPasswordDTO) => {
  return http.request<ResponseData<TokenDTO>>("post", "/login", { data });
};

export const register = (data: RegisterDTO) => {
  return http.request<ResponseData<void>>("post", "/register", { data });
};

export const getLoginUserInfo = () => {
  return http.request<ResponseData<TokenDTO>>("get", "/getLoginUserInfo");
};

export interface RouteMeta {
  id: string;
  title: string;
  icon?: string;
  showLink?: boolean;
  showParent?: boolean;
  auths?: string[];
  rank?: number;
  frameSrc?: string;
  isFrameSrcInternal?: boolean;
}

export type RouteItem = RouteRecordRaw & {
  name?: string;
  path: string;
  meta: RouteMeta;
  children?: RouteItem[];
};

type AsyncRoutesResponse = {
  code: number;
  msg: string;
  data: RouteItem[];
};

const addUniqueId = (routes: RouteItem[]): RouteItem[] => {
  return routes.map(route => {
    const id = `${route.name || ""}${route.path}`;

    if (route.children && route.children.length > 0) {
      route.children = addUniqueId(route.children);
    }

    return {
      ...route,
      meta: {
        ...route.meta,
        id
      }
    };
  });
};

function withId(result: AsyncRoutesResponse) {
  if (result.data) {
    result.data = addUniqueId(result.data);
  }

  return result;
}

export const getAsyncRoutes = async () => {
  const result = await http.request<AsyncRoutesResponse>("get", "/getRouters");
  return withId(result);
};
