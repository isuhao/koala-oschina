/*
 * Copyright (c) Koala 2012-2014 All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openkoala.koala.monitor.task;

import java.util.Timer;
import java.util.TimerTask;

import org.openkoala.koala.monitor.core.MonitorTask;

/**
 * 功能描述：<br />
 *  
 * 创建日期：2013-8-7 上午8:58:32  <br />   
 * 
 * 版权信息：Copyright (c) 2013 Koala All Rights Reserved<br />
 * 
 * 作    者：<a href="mailto:vakinge@gmail.com">vakin jiang</a><br />
 * 
 * 修改记录： <br />
 * 修 改 者    修改日期     文件版本   修改说明	
 */
public abstract class BaseMonitorTask implements MonitorTask {

	protected long delay = 60 * 1000;//延迟执行时间
	
    protected long taskPeriod;
	
    protected Timer checkTimer;
    
    @Override
	public void startup() {
		if(!initConfigOK())return;
		checkTimer = new Timer();
		checkTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				doTask();
			}
		},delay, taskPeriod);
		
	}
    
    protected abstract boolean initConfigOK();
    
    protected abstract void doTask();
}
