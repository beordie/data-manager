package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.model.dmserver.Widget;
import com.tipdm.framework.service.BaseService;

import java.util.List;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
public interface ComponentService extends BaseService<Component, Long> {


    public List<Component> findChild(Long parentId, Boolean inBuilt);

    public List<Component> findChild(Long parentId,Long creatorId);

    public List<Component> findAllInBuiltComponent();

    public void update(Long id, Component component);

    public void modifyCatName(Long catId, String catName);

    public void deleteElement(Long eleId);

    public List<Widget> findWidgetList();

    public void saveWidget(Widget widget);
}
