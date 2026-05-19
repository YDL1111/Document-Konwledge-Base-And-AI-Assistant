package com.docbase.domain.system.dept.query;

import com.docbase.common.core.page.AbstractQuery;
import com.docbase.domain.system.dept.db.SysDeptEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author valarchie
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DeptQuery extends AbstractQuery<SysDeptEntity> {

    private Long deptId;

    private Long parentId;


    @Override
    public QueryWrapper<SysDeptEntity> addQueryCondition() {
        return new QueryWrapper<SysDeptEntity>()
            .eq(parentId != null, "parent_id", parentId);
    }
}
