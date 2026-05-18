package com.docbase.domain.system.menu;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.docbase.common.enums.common.StatusEnum;
import com.docbase.domain.system.menu.command.AddMenuCommand;
import com.docbase.domain.system.menu.command.UpdateMenuCommand;
import com.docbase.domain.system.menu.db.SysMenuEntity;
import com.docbase.domain.system.menu.db.SysMenuService;
import com.docbase.domain.system.menu.dto.MenuDTO;
import com.docbase.domain.system.menu.dto.MenuDetailDTO;
import com.docbase.domain.system.menu.dto.RouterDTO;
import com.docbase.domain.system.menu.model.MenuModel;
import com.docbase.domain.system.menu.model.MenuModelFactory;
import com.docbase.domain.system.menu.query.MenuQuery;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuApplicationService {

    private final SysMenuService menuService;

    private final MenuModelFactory menuModelFactory;

    public List<MenuDTO> getMenuList(MenuQuery query) {
        List<SysMenuEntity> list = menuService.list(query.toQueryWrapper());
        return list.stream().map(MenuDTO::new)
            .sorted(Comparator.comparing(MenuDTO::getRank, Comparator.nullsLast(Integer::compareTo)))
            .collect(Collectors.toList());
    }

    public MenuDetailDTO getMenuInfo(Long menuId) {
        SysMenuEntity byId = menuService.getById(menuId);
        return new MenuDetailDTO(byId);
    }

    public List<Tree<Long>> getDropdownList(SystemLoginUser loginUser) {
        List<SysMenuEntity> menuEntityList =
            loginUser.isAdmin() ? menuService.list() : menuService.getMenuListByUserId(loginUser.getUserId());

        return buildMenuTreeSelect(menuEntityList);
    }

    public void addMenu(AddMenuCommand addCommand) {
        MenuModel model = menuModelFactory.create();
        model.loadAddCommand(addCommand);
        model.checkMenuNameUnique();
        model.checkAddButtonInIframeOrOutLink();
        model.checkAddMenuNotInCatalog();
        model.checkExternalLink();
        model.insert();
    }

    public void updateMenu(UpdateMenuCommand updateCommand) {
        MenuModel model = menuModelFactory.loadById(updateCommand.getMenuId());
        model.loadUpdateCommand(updateCommand);
        model.checkMenuNameUnique();
        model.checkAddButtonInIframeOrOutLink();
        model.checkAddMenuNotInCatalog();
        model.checkExternalLink();
        model.checkParentIdConflict();
        model.updateById();
    }

    public void remove(Long menuId) {
        MenuModel menuModel = menuModelFactory.loadById(menuId);
        menuModel.checkHasChildMenus();
        menuModel.checkMenuAlreadyAssignToRole();
        menuModel.deleteById();
    }

    public List<Tree<Long>> buildMenuTreeSelect(List<SysMenuEntity> menus) {
        TreeNodeConfig config = new TreeNodeConfig();
        config.setIdKey("menuId");
        return TreeUtil.build(menus, 0L, config, (menu, tree) -> {
            tree.setId(menu.getMenuId());
            tree.setParentId(menu.getParentId());
            tree.putExtra("label", menu.getMenuName());
        });
    }

    public List<Tree<Long>> buildMenuEntityTree(SystemLoginUser loginUser) {
        List<SysMenuEntity> allMenus =
            loginUser.isAdmin() ? menuService.list() : menuService.getMenuListByUserId(loginUser.getUserId());

        List<SysMenuEntity> noButtonMenus = allMenus.stream()
            .filter(menu -> !menu.getIsButton())
            .filter(menu -> StatusEnum.ENABLE.getValue().equals(menu.getStatus()))
            .collect(Collectors.toList());

        TreeNodeConfig config = new TreeNodeConfig();
        config.setIdKey("menuId");

        return TreeUtil.build(noButtonMenus, 0L, config, (menu, tree) -> {
            tree.setId(menu.getMenuId());
            tree.setParentId(menu.getParentId());
            tree.putExtra("entity", menu);
            tree.putExtra("auths", collectMenuAuths(allMenus, menu.getMenuId()));
        });
    }

    public List<RouterDTO> buildRouterTree(List<Tree<Long>> trees) {
        List<RouterDTO> routers = new LinkedList<>();
        if (CollUtil.isNotEmpty(trees)) {
            for (Tree<Long> tree : trees) {
                Object entity = tree.get("entity");
                if (entity != null) {
                    RouterDTO routerDTO = new RouterDTO((SysMenuEntity) entity);
                    Object auths = tree.get("auths");
                    if (auths instanceof List) {
                        try {
                            routerDTO.getMeta().setAuths((List<String>) auths);
                        } catch (ClassCastException ex) {
                            log.warn("Failed to cast menu auth list for menuId={}", tree.getId(), ex);
                        }
                    }
                    List<Tree<Long>> children = tree.getChildren();
                    if (CollUtil.isNotEmpty(children)) {
                        routerDTO.setChildren(buildRouterTree(children));
                    }
                    routers.add(routerDTO);
                }
            }
        }
        return routers;
    }

    public List<RouterDTO> getRouterTree(SystemLoginUser loginUser) {
        List<Tree<Long>> trees = buildMenuEntityTree(loginUser);
        return buildRouterTree(trees);
    }

    private List<String> collectMenuAuths(List<SysMenuEntity> allMenus, Long menuId) {
        Set<String> auths = new LinkedHashSet<>();
        collectMenuAuthsRecursive(allMenus, menuId, auths);
        return new ArrayList<>(auths);
    }

    private void collectMenuAuthsRecursive(List<SysMenuEntity> allMenus, Long parentId, Set<String> auths) {
        for (SysMenuEntity menu : allMenus) {
            if (!StatusEnum.ENABLE.getValue().equals(menu.getStatus())) {
                continue;
            }
            if (!parentId.equals(menu.getParentId())) {
                continue;
            }
            if (menu.getPermission() != null && !menu.getPermission().isBlank()) {
                auths.add(menu.getPermission());
            }
            collectMenuAuthsRecursive(allMenus, menu.getMenuId(), auths);
        }
    }
}
