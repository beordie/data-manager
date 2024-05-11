package com.tipdm.framework.repository.dmserver;

import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.persist.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
public interface ComponentRepository extends BaseRepository<Component, Long>{

    public void merge(Component component);

    public Integer countByParentIdAndCreatorId(Long parentId, Long creatorId);

    @Query("from Component where parentId=?1 and inBuilt=?2 order by sequence asc nulls last")
    public List<Component> findByParentId(Long parentId, Boolean inBuilt);

    public List<Component> findByInBuiltTrue();

    @Query("from Component where parentId=?1 and creatorId=?2 and inBuilt=?3 order by sequence asc nulls last")
    public List<Component> findByParentId(Long parentId, Long creatorId, Boolean inBuilt);

    Component findByParentIdAndNameAndCreatorIdAndInBuilt(Long parentId, String name, Long creatorId, Boolean inBuilt);
}
