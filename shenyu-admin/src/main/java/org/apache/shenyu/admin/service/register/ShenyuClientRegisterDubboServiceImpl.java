/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.admin.service.register;

import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.admin.model.entity.MetaDataDO;
import org.apache.shenyu.admin.model.entity.SelectorDO;
import org.apache.shenyu.admin.service.MetaDataService;
import org.apache.shenyu.admin.utils.CommonUpstreamUtils;
import org.apache.shenyu.common.dto.convert.rule.impl.DubboRuleHandle;
import org.apache.shenyu.common.dto.convert.selector.DubboUpstream;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * dubbo service register.
 */
@Service
public class ShenyuClientRegisterDubboServiceImpl extends AbstractShenyuClientRegisterServiceImpl {
    
    @Override
    public String rpcType() {
        return RpcTypeEnum.DUBBO.getName();
    }
    
    @Override
    protected String selectorHandler(final MetaDataRegisterDTO metaDataDTO) {
        return "";
    }
    
    @Override
    protected String ruleHandler() {
        return new DubboRuleHandle().toJson();
    }
    
    @Override
    protected void registerMetadata(final MetaDataRegisterDTO metaDataDTO) {
        MetaDataService metaDataService = getMetaDataService();
        MetaDataDO exist = metaDataService.findByPath(metaDataDTO.getPath());
        metaDataService.saveOrUpdateMetaData(exist, metaDataDTO);
    }
    
    @Override
    protected String buildHandle(final List<URIRegisterDTO> uriList, final SelectorDO selectorDO) {
        String handleAdd;
        List<DubboUpstream> addList = buildDubboUpstreamList(uriList);
        List<DubboUpstream> canAddList = new CopyOnWriteArrayList<>();
        if (StringUtils.isBlank(selectorDO.getHandle())) {
            handleAdd = GsonUtils.getInstance().toJson(addList);
            canAddList = addList;
        } else {
            List<DubboUpstream> existList = GsonUtils.getInstance().fromCurrentList(selectorDO.getHandle(), DubboUpstream.class);
            for (DubboUpstream exist : existList) {
                for (DubboUpstream add : addList) {
                    if (!exist.getUpstreamUrl().equals(add.getUpstreamUrl())) {
                        existList.add(add);
                        canAddList.add(add);
                    }
                }
            }
            handleAdd = GsonUtils.getInstance().toJson(existList);
        }
        doSubmit(selectorDO.getId(), canAddList);
        return handleAdd;
    }
    
    private List<DubboUpstream> buildDubboUpstreamList(final List<URIRegisterDTO> uriList) {
        return uriList.stream()
                .map(dto -> CommonUpstreamUtils.buildDefaultDubboUpstream(dto.getHost(), dto.getPort()))
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }
}
