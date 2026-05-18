import { getQueryMap, subBefore } from "@pureadmin/utils";
import { removeToken, type DataInfo } from "./auth";

/**
 * 兼容通过 URL 参数带 token 的单点登录跳转。
 * 当前项目登录态由后端 `/login` 返回的完整 `TokenDTO` 建立，
 * 因此前端这里不再直接写 token，只做参数清理，避免类型错误和脏地址残留。
 */
(function () {
  const params = getQueryMap(location.href) as Partial<DataInfo<Date>>;
  const requiredFields = ["username", "roles", "accessToken"];

  if (!requiredFields.every(field => field in params)) {
    return;
  }

  removeToken();

  delete params.roles;
  delete params.accessToken;

  const queryString = JSON.stringify(params)
    .replace(/["{}]/g, "")
    .replace(/:/g, "=")
    .replace(/,/g, "&");

  const newUrl = `${location.origin}${location.pathname}${subBefore(
    location.hash,
    "?"
  )}${queryString ? `?${queryString}` : ""}`;

  window.location.replace(newUrl);
})();
