<%@ page language="java" contentType="text/html; charset=UTF-8"  pageEncoding="UTF-8"%>
<script>
	$(function() {
		var tabData 	= $('.tab-pane.active').data();
		var userId 		= tabData.userId;
		var columns = [{
			title : "角色名称",
			name : "roleName",
			width : 250
		}, {
			title : "角色描述",
			name : "description",
			width : 250
		}];
		
		var buttons = (function() {
			if (userId) {
				return [{
					content : '<button class="btn btn-primary" type="button"><span class="glyphicon glyphicon-th-large"><span>分配角色</button>',
					action : 'assignRole'
				}, {
					content : '<button class="btn btn-danger" type="button"><span class="glyphicon glyphicon-remove"><span>删除</button>',
					action : 'removeRoleForUser'
				}];
			} else {
				return [{
					content : '<button class="btn btn-primary" type="button"><span class="glyphicon glyphicon-plus"><span>添加</button>',
					action : 'add'
				}, {
					content : '<button class="btn btn-success" type="button"><span class="glyphicon glyphicon-edit"><span>修改</button>',
					action : 'modify'
				}, {
					content : '<button class="btn btn-danger" type="button"><span class="glyphicon glyphicon-remove"><span>删除</button>',
					action : 'delete'
				}, {
					content : '<button class="btn btn-info" type="button"><span class="glyphicon glyphicon-th-large"></span>&nbsp;分配url</button>',
					action : 'urlAssign'
				}, {
					content : '<button class="btn btn-info" type="button"><span class="glyphicon glyphicon-th-large"></span>&nbsp;分配menu</button>',
					action : 'menuAssign'
				}];
			}
		})();
		
		var url = contextPath + '/auth/role/pagingquery.koala';
		if (userId) {
			url = contextPath + '/auth/user/pagingQueryGrantRoleByUserId.koala?userId=' + userId;
		}
		
		$("<div/>").appendTo($("#tabContent>div:last-child")).grid({
			identity : 'id',
			columns : columns,
			buttons : buttons,
			url 	: url
		}).on({
			'add' 	: function() {
				roleManager().add($(this));
			},
			'modify' : function(event, data) {
				var indexs = data.data;
				var $this = $(this);
				if (indexs.length == 0) {
					$this.message({
						type : 'warning',
						content : '请选择一条记录进行修改'
					});
					return;
				}
				if (indexs.length > 1) {
					$this.message({
						type : 'warning',
						content : '只能选择一条记录进行修改'
					});
					return;
				}
				roleManager().modify(data.item[0], $(this));
			},
			'delete' : function(event, data) {
				var indexs = data.data;
				var $this = $(this);
				if (indexs.length == 0) {
					$this.message({
						type : 'warning',
						content : '请选择要删除的记录'
					});
					return;
				}
				$this.confirm({
					content : '确定要删除所选记录吗?',
					callBack : function() {
						roleManager().deleteRole(data.item, $this);
					}
				});
			},
			'assignRole' : function() {
				var grid = $(this);
        		$.get(contextPath + '/pages/auth/select-role.jsp').done(function(data){
        			var dialog = $(data);
        			dialog.find('#save').click(function(){
        				var saveBtn = $(this);
        				var items = dialog.find('.selectRoleGrid').data('koala.grid').selectedRows();
        				
        				if(items.length == 0){
        					dialog.find('.selectRoleGrid').message({
        						type: 'warning',
        						content: '请选择要分配的角色'
        					});
        					return;
        				}
        				
        				saveBtn.attr('disabled', 'disabled');	
        				var data = "userId="+userId;
        				
        				console.log(items);
        				
        				for(var i=0,j=items.length; i<j; i++){
        					data += "&roleIds="+items[i].roleId;
        				}
        				
        				$.post(contextPath + '/auth/user/grantRoles.koala', data).done(function(data){
       						grid.message({
       							type: 'success',
       							content: '保存成功'
       						});
       						dialog.modal('hide');
       						grid.grid('refresh');
        				}).fail(function(data){
        					saveBtn.attr('disabled', 'disabled');	
        					grid.message({
        						type: 'error',
        						content: '保存失败'
        					});
        				});
        			}).end().modal({
        				keyboard: false
        			}).on({
       					'hidden.bs.modal': function(){
       						$(this).remove();
       					},
       					
       					'shown.bs.modal': function(){ //弹窗初始化完毕后，初始化url选择表格
       						var columns = [{
       							title : "角色名称",
       							name : "roleName",
       							width : 250
       						}, {
       							title : "角色描述",
       							name : "description",
       							width : 250
       						}];
       					
        					dialog.find('.selectRoleGrid').grid({
        						 identity: 'id',
        			             columns: columns,
        			             querys: [{title: 'roleId', value: 'roleId'}],
        			             url: contextPath + '/auth/user/pagingQueryNotGrantRoles.koala?userId='+userId
        			        });
       					},
       					
       					'complete': function(){
       						grid.message({
       							type: 'success',
       							content: '保存成功'
       						});
       						$(this).modal('hide');
       						grid.grid('refresh');
       					}
        			});
        			 //兼容IE8 IE9
        	        if(window.ActiveXObject){
        	           if(parseInt(navigator.userAgent.toLowerCase().match(/msie ([\d.]+)/)[1]) < 10){
        	        	   dialog.trigger('shown.bs.modal');
        	           }
        	        }
        		});
			},
			"urlAssign" : function(event, data){
				var items 	= data.item;
				var thiz	= $(this);
				if(items.length == 0){
					thiz.message({type : 'warning',content : '请选择一条记录进行操作'});
					return;
				} else if(items.length > 1){
					thiz.message({type : 'warning',content : '只能选择一条记录进行操作'});
					return;
				}
				
				var role = items[0];
				/*打开url表格*/
				openTab('/pages/auth/url-list.jsp', role.roleName+'的url管理', 'roleManager_' + role.id, role.id, {roleId : role.roleId});
			},
			"menuAssign" : function(event, data) {
				var items = data.item;
				var $this = $(this);
				if (items.length == 0) {
					$this.message({
						type : 'warning',
						content : '请选择一条记录进行操作'
					});
					return;
				}
				if (items.length > 1) {
					$this.message({
						type : 'warning',
						content : '只能选择一条记录进行操作'
					});
					return;
				}
				roleManager().assignResource($(this), items[0].roleId);
			},
			'removeRoleForUser' : function(event, data) {
				var indexs = data.data;
				var grid = $(this);
				if (indexs.length == 0) {
					$this.message({
						type : 'warning',
						content : '请选择要删除的记录'
					});
					return;
				}
				grid.confirm({
					content : '确定要删除所选记录吗?',
					callBack : function() {
						var url = contextPath + '/auth/user/terminateRolesByUser.koala';
						var params = "userId="+userId;
						for (var i = 0, j = data.item.length; i < j; i++) {
							params += ("&roleIds=" + data.item[i].roleId);
						}
						
						$.post(url, params).done(function(data){
							if(data.success){
								grid.message({
									type: 'success',
									content: '删除成功'
								});
								grid.grid('refresh');
							}else{
								grid.message({
									type: 'error',
									content: data.actionError
								});
							}
						}).fail(function(data){
							grid.message({
								type: 'error',
								content: '删除失败'
							});
						});
					}
				});
			},
			'assignResource' : function(event, data) {
				var indexs = data.data;
				var $this = $(this);
				if (indexs.length == 0) {
					$this.message({
						type : 'warning',
						content : '请选择一条记录进行操作'
					});
					return;
				}
				if (indexs.length > 1) {
					$this.message({
						type : 'warning',
						content : '只能选择一条记录进行操作'
					});
					return;
				}
				roleManager().assignResource($(this), data.data[0]);
			}
		});
	});
</script>