package org.openkoala.security.org.facade.impl;

import org.apache.commons.lang3.StringUtils;
import org.dayatang.domain.InstanceFactory;
import org.dayatang.querychannel.Page;
import org.dayatang.querychannel.QueryChannelService;
import org.openkoala.koala.commons.InvokeResult;
import org.openkoala.security.application.SecurityAccessApplication;
import org.openkoala.security.core.domain.Authorization;
import org.openkoala.security.core.domain.Permission;
import org.openkoala.security.core.domain.Role;
import org.openkoala.security.core.domain.User;
import org.openkoala.security.facade.SecurityAccessFacade;
import org.openkoala.security.facade.dto.PermissionDTO;
import org.openkoala.security.org.core.domain.EmployeeUser;
import org.openkoala.security.org.core.domain.OrganisationScope;
import org.openkoala.security.org.facade.SecurityOrgAccessFacade;
import org.openkoala.security.org.facade.dto.EmployeeUserDTO;
import org.openkoala.security.org.facade.dto.OrgPermissionDTO;
import org.openkoala.security.org.facade.dto.OrgRoleDTO;
import org.openkoala.security.org.facade.dto.OrganizationScopeDTO;
import org.openkoala.security.org.facade.impl.assembler.EmployeeUserAssembler;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.*;

@Named
public class SecurityOrgAccessFacadeImpl implements SecurityOrgAccessFacade {

    @Inject
    private SecurityAccessApplication securityAccessApplication;

    @Override
    public InvokeResult pagingQueryEmployeeUsers(int pageIndex, int pageSize, EmployeeUserDTO queryEmployeeUserCondition) {
        Map<String, Object> conditionVals = new HashMap<String, Object>();
        StringBuilder jpql = new StringBuilder("FROM EmployeeUser _user where 1=1");
        assembleUserJpqlAndConditionValues(queryEmployeeUserCondition, jpql, "_user", conditionVals);

        Page<EmployeeUser> results = getQueryChannelService().createJpqlQuery(jpql.toString())//
                .setParameters(conditionVals)//
                .setPage(pageIndex, pageSize)//
                .pagedList();


        return InvokeResult.success(new Page<EmployeeUserDTO>(results.getStart(), results.getResultCount(), pageSize, transformToEmployeeUserDTO(results.getData())));
    }

    private List<EmployeeUserDTO> transformToEmployeeUserDTO(List<EmployeeUser> employeeUsers) {
        List<EmployeeUserDTO> results = new ArrayList<EmployeeUserDTO>();
        for (EmployeeUser employeeUser : employeeUsers) {
            EmployeeUserDTO result = EmployeeUserAssembler.toEmployeeUserDTO(employeeUser);
            // 这个地方直接使用用户管理的员工的组织的名称。因为组织的名称和范围的名称一直。保存同步
            if(null != employeeUser.getEmployee()){
                result.setEmployeeName(employeeUser.getEmployee().getName());
                result.setEmployeeOrgName(employeeUser.getEmployee().getOrganization(new Date()).getName());
            }
            results.add(result);
        }
        return results;
    }

    private List<OrgRoleDTO> transformToOrgRoleDTO(List<Authorization> authorizations){
        List<OrgRoleDTO> results = new ArrayList<OrgRoleDTO>();
        for(Authorization authorization : authorizations){
            OrgRoleDTO result = null;
            if(authorization.getAuthority() instanceof  Role){
                Role role = (Role) authorization.getAuthority();
                result = new OrgRoleDTO(role.getId(),role.getName(),role.getDescription());
            }
            // 直接使用范围的名称，因为范围保持一致。
            if(authorization.getScope() instanceof OrganisationScope){
                OrganisationScope scope = (OrganisationScope) authorization.getScope();
                if(result != null){
                    result.setEmployeeUserOrgName(scope.getName());
                }
            }
            results.add(result);
        }
        return results;
    }

    private List<OrgPermissionDTO> transformToOrgPermissionDTO(List<Authorization> authorizations){
        List<OrgPermissionDTO> results = new ArrayList<OrgPermissionDTO>();
        for(Authorization authorization : authorizations){
            OrgPermissionDTO result = null;
            if(authorization.getAuthority() instanceof  Permission){
                Permission permission = (Permission) authorization.getAuthority();
                result = new OrgPermissionDTO(permission.getId(),permission.getName(),permission.getIdentifier(),permission.getDescription());
            }
            // 直接使用范围的名称，因为范围保持一致。
            if(authorization.getScope() instanceof OrganisationScope){
                OrganisationScope scope = (OrganisationScope) authorization.getScope();
                if(result != null){
                    result.setEmployeeUserOrgName(scope.getName());
                }
            }
            results.add(result);
        }
        return results;
    }

    private void addOrganizationScopeChildrenToParent(Set<OrganizationScopeDTO> all) {
        LinkedHashMap<Long, OrganizationScopeDTO> map = new LinkedHashMap<Long, OrganizationScopeDTO>();
        for (OrganizationScopeDTO organizationScopeDTO : all) {
            map.put(organizationScopeDTO.getId(), organizationScopeDTO);
        }
        for (OrganizationScopeDTO organizationScopeDTO : map.values()) {
            Long parentId = organizationScopeDTO.getParentId();
            if (!StringUtils.isBlank(parentId + "") && map.get(parentId) != null) {
                map.get(parentId).getChildren().add(organizationScopeDTO);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<OrganizationScopeDTO> findChidrenOrganizationScopes() {

        StringBuilder jpql = new StringBuilder(
                "SELECT NEW org.openkoala.security.facade.dto.OrganizationScopeDTO(_organizationScope.id, _organizationScope.name, _organizationScope.description, _organizationScope.parent.id) FROM OrganisationScope _organizationScope");
        jpql.append(" WHERE _organizationScope.level > :level");
        jpql.append(" GROUP BY _organizationScope.id");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("level", 0);

        List<OrganizationScopeDTO> results = getQueryChannelService()//
                .createJpqlQuery(jpql.toString())//
                .setParameters(parameters)//
                .list();

        return results;
    }

    private List<OrganizationScopeDTO> findTopOrganizationScopes() {
        StringBuilder jpql = new StringBuilder(
                "SELECT NEW org.openkoala.security.facade.dto.OrganizationScopeDTO(_organizationScope.id, _organizationScope.name, _organizationScope.description, _organizationScope.parent.id) FROM OrganisationScope _organizationScope");
        jpql.append(" WHERE _organizationScope.parent IS NULL");
        jpql.append(" AND _organizationScope.level = :level");
        jpql.append(" GROUP BY _organizationScope.id");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("level", 0);

        List<OrganizationScopeDTO> results = getQueryChannelService()//
                .createJpqlQuery(jpql.toString())//
                .setParameters(parameters)//
                .list();

        return results;
    }

    private QueryChannelService queryChannelService;

    public QueryChannelService getQueryChannelService() {
        if (queryChannelService == null) {
            queryChannelService = InstanceFactory.getInstance(QueryChannelService.class,"queryChannel_security");
        }
        return queryChannelService;
    }

    private void assembleUserJpqlAndConditionValues(EmployeeUserDTO queryEmployeeUserCondition, StringBuilder jpql, String conditionPrefix, Map<String, Object> conditionVals) {

        String andCondition = " AND " + conditionPrefix;

        if (null != queryEmployeeUserCondition.getDisabled() && !"".equals(queryEmployeeUserCondition.getDisabled())) {
            jpql.append(andCondition);
            jpql.append(".disabled = :disabled");
            conditionVals.put("disabled", queryEmployeeUserCondition.getDisabled());
        }

        if (!StringUtils.isBlank(queryEmployeeUserCondition.getName())) {
            jpql.append(andCondition);
            jpql.append(".name LIKE :name");
            conditionVals.put("name", MessageFormat.format("%{0}%", queryEmployeeUserCondition.getName()));
        }
        if (!StringUtils.isBlank(queryEmployeeUserCondition.getUserAccount())) {
            jpql.append(andCondition);
            jpql.append(".userAccount LIKE :userAccount");
            conditionVals.put("userAccount", MessageFormat.format("%{0}%", queryEmployeeUserCondition.getUserAccount()));
        }
        if (!StringUtils.isBlank(queryEmployeeUserCondition.getEmail())) {
            jpql.append(andCondition);
            jpql.append(".email LIKE :email");
            conditionVals.put("email", MessageFormat.format("%{0}%", queryEmployeeUserCondition.getEmail()));
        }
        if (!StringUtils.isBlank(queryEmployeeUserCondition.getTelePhone())) {
            jpql.append(andCondition);
            jpql.append(".telePhone LIKE :telePhone");
            conditionVals.put("telePhone", MessageFormat.format("%{0}%", queryEmployeeUserCondition.getTelePhone()));
        }
        if (!StringUtils.isBlank(queryEmployeeUserCondition.getEmployeeName())) {
            jpql.append("AND .employee.name LIKE :employeeName");
            conditionVals.put("employeeName", MessageFormat.format("%{0}%", queryEmployeeUserCondition.getEmployeeName()));
        }
    }

    @Override
    public InvokeResult pagingQueryGrantRolesByUserId(int pageIndex, int pageSize, Long userId) {

//        StringBuilder jpql = new StringBuilder("SELECT _authorization FROM Authorization _authorization JOIN _authorization.actor _actor JOIN _authorization.authority _authority");
//        jpql.append(" WHERE TYPE(_authority) = :authorityType");
//        jpql.append(" AND _actor.id = :userId");
//        Map<String, Object> parameters = new HashMap<String, Object>();
//        parameters.put("authorityType", Role.class);
//        parameters.put("userId", userId);


//        Page<Authorization> results = getQueryChannelService()//
//                .createJpqlQuery(jpql.toString())//
//                .setParameters(parameters)//
//                .setPage(pageIndex, pageSize)//
//                .pagedList();

        User user = securityAccessApplication.getUserById(userId);

        Page<Authorization> results = getQueryChannelService()//
                .createJpqlQuery(getAuthorizationsByActorJpql())//
                .addParameter("authorityType", Role.class)//
                .addParameter("actor", user)
                .setPage(pageIndex, pageSize)//
                .pagedList();

        return InvokeResult.success(new Page<OrgRoleDTO>(results.getStart(), results.getResultCount(), pageSize, transformToOrgRoleDTO(results.getData())));
    }

    /**
     * 这个地方使用命名查询需要配置
     * @param pageIndex
     * @param pageSize
     * @param userId
     * @return
     */
    @Override
    public InvokeResult pagingQueryGrantPermissionsByUserId(int pageIndex, int pageSize, Long userId) {
        User user = securityAccessApplication.getUserById(userId);
        Page<Authorization> results = getQueryChannelService()//
                .createJpqlQuery(getAuthorizationsByActorJpql())//
                .addParameter("authorityType", Permission.class)//
                .addParameter("actor", user)
                .setPage(pageIndex, pageSize)//
                .pagedList();

        return InvokeResult.success(new Page<OrgPermissionDTO>(results.getStart(), results.getResultCount(), pageSize, transformToOrgPermissionDTO(results.getData())));

    }

    private String getAuthorizationsByActorJpql() {
        return new StringBuilder("SELECT _authorization FROM Authorization _authorization JOIN _authorization.authority _authority JOIN _authorization.actor _actor WHERE _actor = :actor AND TYPE(_authority) = :authorityType").toString();
    }

}