package org.openkoala.security.org.facade.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.dayatang.domain.InstanceFactory;
import org.dayatang.querychannel.QueryChannelService;
import org.openkoala.koala.commons.InvokeResult;
import org.openkoala.organisation.application.BaseApplication;
import org.openkoala.organisation.application.OrganizationApplication;
import org.openkoala.organisation.core.domain.Employee;
import org.openkoala.organisation.core.domain.Organization;
import org.openkoala.security.application.SecurityAccessApplication;
import org.openkoala.security.application.SecurityConfigApplication;
import org.openkoala.security.application.SecurityDBInitApplication;
import org.openkoala.security.core.NullArgumentException;
import org.openkoala.security.core.UserAccountIsExistedException;
import org.openkoala.security.core.domain.Actor;
import org.openkoala.security.core.domain.Authority;
import org.openkoala.security.core.domain.MenuResource;
import org.openkoala.security.core.domain.PageElementResource;
import org.openkoala.security.core.domain.Permission;
import org.openkoala.security.core.domain.Role;
import org.openkoala.security.core.domain.Scope;
import org.openkoala.security.core.domain.SecurityResource;
import org.openkoala.security.core.domain.UrlAccessResource;
import org.openkoala.security.org.core.domain.EmployeeUser;
import org.openkoala.security.org.core.domain.OrganisationScope;
import org.openkoala.security.org.facade.SecurityOrgConfigFacade;
import org.openkoala.security.org.facade.command.ChangeEmployeeUserPropsCommand;
import org.openkoala.security.org.facade.command.CreateEmpolyeeUserCommand;
import org.openkoala.security.org.facade.command.TerminateUserFromPermissionInScopeCommand;
import org.openkoala.security.org.facade.command.TerminateUserFromRoleInScopeCommand;
import org.openkoala.security.org.facade.dto.AuthorizationCommand;
import org.openkoala.security.org.facade.impl.assembler.EmployeeUserAssembler;
import org.openkoala.security.org.facade.impl.systeminit.SystemInit;
import org.openkoala.security.org.facade.impl.systeminit.SystemInitFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(value = "transactionManager_security")
@Named
public class SecurityOrgConfigFacadeImpl implements SecurityOrgConfigFacade {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityOrgConfigFacadeImpl.class);

	private static SystemInit systemInit = SystemInitFactory.INSTANCE.getSystemInit();
	
    @Inject
    private BaseApplication baseApplication;

    @Inject
    private SecurityConfigApplication securityConfigApplication;

    @Inject
    private SecurityAccessApplication securityAccessApplication;

    @Inject
    private OrganizationApplication organizationApplication;

    @Inject
    private SecurityDBInitApplication securityDBInitApplication;

    private QueryChannelService queryChannelService;

    public QueryChannelService getQueryChannelService() {
        if (queryChannelService == null) {
            queryChannelService = InstanceFactory.getInstance(QueryChannelService.class, "queryChannel_security");
        }
        return queryChannelService;
    }

	@Override
	public InvokeResult createEmployeeUser(CreateEmpolyeeUserCommand command) {
		try {
			EmployeeUser employeeUser = EmployeeUserAssembler.toEmployeeUser(command);
            // 可能不选择员工。
            if(command.getEmployeeId() != null){
                Employee employee =  baseApplication.getEntity(Employee.class,command.getEmployeeId());
                employeeUser.setEmployee(employee);
            }
            securityConfigApplication.createActor(employeeUser);
            return InvokeResult.success();
		} catch (NullArgumentException e) {
			LOGGER.error(e.getMessage(), e);
			return InvokeResult.failure("名称或者账户不能为空。");
		} catch (UserAccountIsExistedException e) {
			LOGGER.error(e.getMessage(), e);
			return InvokeResult.failure("账号已经存在。");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return InvokeResult.failure("添加用户失败。");
		}
	}

    @Override
    public InvokeResult terminateUserFromRoleInScope(Long userId, TerminateUserFromRoleInScopeCommand[] commands) {
        EmployeeUser employeeUser = securityAccessApplication.getActorById(userId);
        for (TerminateUserFromRoleInScopeCommand command : commands) {
            Long roleId = command.getRoleId();
            Long scopeId = command.getScopeId();
            Role role = securityAccessApplication.getRoleBy(roleId);
            OrganisationScope scope = securityAccessApplication.getScope(scopeId);
            securityConfigApplication.terminateActorFromAuthorityInScope(employeeUser, role, scope);
        }
        return InvokeResult.success();
    }

    @Override
    public InvokeResult terminateUserFromPermissionInScope(Long userId, TerminateUserFromPermissionInScopeCommand[] commands) {
        EmployeeUser employeeUser = securityAccessApplication.getActorById(userId);
        for (TerminateUserFromPermissionInScopeCommand command : commands) {
            Long permissionId = command.getPermissionId();
            Long scopeId = command.getScopeId();
            Permission permission = securityAccessApplication.getPermissionBy(permissionId);
            OrganisationScope scope = securityAccessApplication.getScope(scopeId);
            securityConfigApplication.terminateActorFromAuthorityInScope(employeeUser, permission, scope);
        }
        return InvokeResult.success();
    }

    @Override
    public InvokeResult grantRolesToUserInScope(AuthorizationCommand command) {
        Actor actor = securityAccessApplication.getActorById(command.getActorId());

        Organization organization = organizationApplication.getOrganizationById(command.getOrganizationId());
        Scope scope = findOrganizationScope(organization);

        if(scope == null){
            scope = new OrganisationScope(command.getOrganizationName(),organization);
            securityConfigApplication.createScope(scope);
        }

        for(Long authorityId : command.getAuthorityIds()){
            Authority authority = securityAccessApplication.getAuthority(authorityId);
            securityConfigApplication.grantActorToAuthorityInScope(actor,authority,scope);
        }

        return InvokeResult.success();
    }

    private OrganisationScope findOrganizationScope(Organization organization){
       OrganisationScope organisationScope = (OrganisationScope) getQueryChannelService()//
                .createNamedQuery("OrganisationScope.hasOrganizationOfScope")//
                .addParameter("organization",organization)//
                .singleResult();
        return organisationScope;
    }

    @Override
    public InvokeResult changeEmployeeUserProps(ChangeEmployeeUserPropsCommand command) {
        try {
            EmployeeUser employeeUser = securityAccessApplication.getActorById(command.getId());
            // 可能不选择员工。
            if(command.getEmployeeId() != null){
                Employee employee =  baseApplication.getEntity(Employee.class,command.getEmployeeId());
                employeeUser.setEmployee(employee);
            }
            employeeUser.setName(command.getName());
            employeeUser.setDescription(command.getDescription());

            securityConfigApplication.changeLastModifyTimeOfUser(employeeUser);

            securityConfigApplication.createActor(employeeUser);
            return InvokeResult.success();
        } catch (NullArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            return InvokeResult.failure("名称或者账户不能为空。");
        } catch (UserAccountIsExistedException e) {
            LOGGER.error(e.getMessage(), e);
            return InvokeResult.failure("账号已经存在。");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return InvokeResult.failure("添加用户失败。");
        }
    }

    @Override
    public void initSecurityOrgSystem() {
        if(securityAccessApplication.hasUserExisted()){
            return;
        }
        EmployeeUser employeeUser = intiEmployeeUser();
        Role role = securityDBInitApplication.initRole();
        List<MenuResource> menuResources = initMenuResources();
        List<PageElementResource> pageElementResources = initPageElementResources();
        List<UrlAccessResource> urlAccessResources = initUrlAccessResources();
        securityConfigApplication.grantAuthorityToActor(role,employeeUser);
        securityConfigApplication.grantSecurityResourcesToAuthority(menuResources,role);
        securityConfigApplication.grantSecurityResourcesToAuthority(pageElementResources,role);
        securityConfigApplication.grantSecurityResourcesToAuthority(urlAccessResources,role);
    }
    
    @Override
    public InvokeResult grantAuthorityToActorInScope(AuthorizationCommand command) {
        Actor actor = securityAccessApplication.getActorById(command.getActorId());
        for(Long authorityId : command.getAuthorityIds()) {
            Authority authority = securityAccessApplication.getAuthority(authorityId);
            if(command.getOrganizationId() != null) {
                Organization organization = organizationApplication.getOrganizationById(command.getOrganizationId());
                Scope scope = findOrganizationScope(organization);
                if(scope == null){
                    scope = new OrganisationScope(command.getOrganizationName(),organization);
                    securityConfigApplication.createScope(scope);
                }
                securityConfigApplication.grantActorToAuthorityInScope(actor,authority,scope);
            }else{
                securityConfigApplication.grantAuthorityToActor(authority,actor);
            }
        }
        return InvokeResult.success();
    }

    private EmployeeUser intiEmployeeUser() {
    	SystemInit.User initEmployeeUser= systemInit.getUser();
        EmployeeUser employeeUser = new EmployeeUser(initEmployeeUser.getName(), initEmployeeUser.getUsername());
        employeeUser.setCreateOwner(initEmployeeUser.getCreateOwner());
        employeeUser.setDescription(initEmployeeUser.getDescription());
        securityConfigApplication.createActor(employeeUser);
        return employeeUser;
    }
    
    public List<MenuResource> initMenuResources() {
    	List<MenuResource> menuResources = new ArrayList<MenuResource>();
    	for (SystemInit.MenuResource each : getParentMenuResources()) {
    		MenuResource menuResource = transformMenuResourceEntity(each);
    		securityConfigApplication.createSecurityResource(menuResource);
    		menuResources.add(menuResource);
    		createChildrenMenuResource(menuResource, each, menuResources);
    	}
        return menuResources;
    }
    
    public List<PageElementResource> initPageElementResources() {
    	List<PageElementResource> results = new ArrayList<PageElementResource>();
    	for (SystemInit.PageElementResource each : systemInit.getPageElementResource()) {
    		PageElementResource pageElementResource = new PageElementResource(each.getName(), each.getUrl());
    		results.add(pageElementResource);
    	}
    	SecurityResource.batchSave(results);

//        // 删除授权权限。
//        Iterator<PageElementResource> resultIterator = results.listIterator();
//        while(resultIterator.hasNext()){
//            PageElementResource result = resultIterator.next();
//            if(result.getIdentifier().contains("GrantPermission")){
//                resultIterator.remove();
//            }
//        }

        return results;
    }

    public List<UrlAccessResource> initUrlAccessResources() {
    	List<UrlAccessResource> results = new ArrayList<UrlAccessResource>();
    	for (SystemInit.UrlAccessResource each : systemInit.getUrlAccessResource()) {
    		UrlAccessResource urlAccessResource = new UrlAccessResource(each.getName(), each.getUrl());
    		results.add(urlAccessResource);
    	}
        SecurityResource.batchSave(results);
        return results;
    }
    
    private MenuResource transformMenuResourceEntity(SystemInit.MenuResource initMenuResource) {
		MenuResource menuResource = new MenuResource(initMenuResource.getName());
		menuResource.setDescription(initMenuResource.getDescription());
		menuResource.setMenuIcon(initMenuResource.getMenuIcon());
		menuResource.setUrl(initMenuResource.getUrl());
		return menuResource;
	}
    
    private void createChildrenMenuResource(MenuResource menuResource, SystemInit.MenuResource parentMenuResource, List<MenuResource> menuResources) {
    	for (SystemInit.MenuResource each : systemInit.getMenuResource()) {
    		if (Integer.valueOf(parentMenuResource.getId()).equals(each.getPid())) {
    			MenuResource children = transformMenuResourceEntity(each);
    			menuResources.add(children);
    			securityConfigApplication.createChildToParent(children, menuResource.getId());
    			createChildrenMenuResource(children, each, menuResources);
    		}
    	}
	}

    private List<SystemInit.MenuResource> getParentMenuResources() {
    	List<SystemInit.MenuResource> parentMenuResources = new ArrayList<SystemInit.MenuResource>();
    	for (SystemInit.MenuResource each : systemInit.getMenuResource()) {
    		if (each.getPid() == null) {
    			parentMenuResources.add(each);
    		}
    	}
    	return parentMenuResources;
    }

}