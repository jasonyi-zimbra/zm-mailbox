/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateDistributionList extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
	    
        String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
        Map<String, Object> attrs = AdminService.getAttrs(request, true);
        
        boolean dynamic = request.getAttributeBool(AdminConstants.A_DYNAMIC, false);

        if (dynamic) {
            checkDomainRightByEmail(zsc, name, Admin.R_createGroup);
            checkSetAttrsOnCreate(zsc, TargetType.group, name, attrs);
        } else {
            checkDomainRightByEmail(zsc, name, Admin.R_createDistributionList);
            checkSetAttrsOnCreate(zsc, TargetType.dl, name, attrs);
        }
        
        Group group = prov.createGroup(name, attrs, dynamic);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                 new String[] {"cmd", "CreateDistributionList","name", name}, attrs));         

        Element response = zsc.createElement(AdminConstants.CREATE_DISTRIBUTION_LIST_RESPONSE);
        
        GetDistributionList.encodeDistributionList(response, group);

        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createDistributionList);
        relatedRights.add(Admin.R_createGroup);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyDistributionList.getName(), "distribution list"));
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyGroup.getName(), "group"));
    }
}
