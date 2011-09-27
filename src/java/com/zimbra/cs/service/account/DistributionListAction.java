package com.zimbra.cs.service.account;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.soap.ZimbraSoapContext;

public class DistributionListAction extends DistributionListDocumentHandler {
    
    private static enum DLAction {
        delete,
        modify,
        rename,
        addAlias,
        removeAlias,
        addOwner,
        removeOwner,
        addMembers,
        removeMembers;
        
        private static DLAction fromString(String str) throws ServiceException {
            try {
                return DLAction.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid op: " + str, e);
            }
        }
    };
    
    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getRequestedAccount(zsc);

        Group group = getGroup(request, acct, prov);
        
        Element eAction = request.getElement(AccountConstants.E_ACTION);
        DLAction op = DLAction.fromString(eAction.getAttribute(AccountConstants.A_OP));
        
        
        DLActionHandler handler = null;
        switch (op) {
            case delete:
                handler = new DeleteHandler(eAction, group, acct);
                break;
            case modify:
                handler = new ModifyHandler(eAction, group, acct);
                break;
            case rename:
                handler = new RenameHandler(eAction, group, acct);
                break;
            case addAlias:
                handler = new AddAliasHandler(eAction, group, acct);
                break;
            case removeAlias:
                handler = new RemoveAliasHandler(eAction, group, acct);
                break;
            case addOwner:
                handler = new AddOwnerHandler(eAction, group, acct);
                break;
            case removeOwner:
                handler = new RemoveOwnerHandler(eAction, group, acct);
                break;
            case addMembers:
                handler = new AddMembersHandler(eAction, group, acct);
                break;
            case removeMembers:
                handler = new RemoveMembersHandler(eAction, group, acct);
                break;
            default:
                throw ServiceException.FAILURE("unsupported op:" + op.name(), null);
        }
        
        handler.handle();
        
        Element response = zsc.createElement(AccountConstants.DISTRIBUTION_LIST_ACTION_RESPONSE);

        return response;
    }
    
    
    private static abstract class DLActionHandler {
        protected Element eAction;
        protected Group group;
        protected Account requestedAcct;
        protected Provisioning prov;
        
        protected DLActionHandler(Element request, Group group, Account requestedAcct) {
            this.eAction = request;
            this.group = group;
            this.requestedAcct = requestedAcct;
            this.prov = Provisioning.getInstance();
        }
        
        abstract void handle() throws ServiceException;
        abstract DLAction getAction();
    }
    
    private static class DeleteHandler extends DLActionHandler {

        protected DeleteHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }

        @Override
        DLAction getAction() {
            return DLAction.delete;
        }
        
        @Override
        void handle() throws ServiceException {
            prov.deleteGroup(group.getId());
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "id", group.getId()}));
        }

    }
    
    private static class ModifyHandler extends DLActionHandler {

        protected ModifyHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.modify;
        }

        @Override
        void handle() throws ServiceException {
            Map<String, Object> attrs = AccountService.getAttrs(
                    eAction, true, AccountConstants.A_N);
            prov.modifyAttrs(group, attrs, true);    
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName()}, attrs)); 
        }
    }
    
    private static class RenameHandler extends DLActionHandler {

        protected RenameHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.rename;
        }

        @Override
        void handle() throws ServiceException {
            Element eNewName = eAction.getElement(AccountConstants.E_NEW_NAME);
            String newName = eNewName.getText();
            
            String oldName = group.getName();
            prov.renameGroup(group.getId(), newName);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", oldName, "newName", newName})); 
        }
    }
    
    private static class AddAliasHandler extends DLActionHandler {

        protected AddAliasHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.addAlias;
        }

        @Override
        void handle() throws ServiceException {
            String alias = eAction.getAttribute(AccountConstants.E_ALIAS);
            prov.addGroupAlias(group, alias);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "alias", alias})); 
        }
    }
    
    private static class RemoveAliasHandler extends DLActionHandler {

        protected RemoveAliasHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.removeAlias;
        }

        @Override
        void handle() throws ServiceException {
            String alias = eAction.getAttribute(AccountConstants.E_ALIAS);
            prov.removeGroupAlias(group, alias);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "alias", alias})); 
        }
    }
    
    static class AddOwnerHandler extends DLActionHandler {

        protected AddOwnerHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.addOwner;
        }

        @Override
        void handle() throws ServiceException {
            Element eOwner = eAction.getElement(AccountConstants.E_OWNER);
            GranteeType ownerType = GranteeType.fromCode(eOwner.getAttribute(AccountConstants.A_TYPE));
            GranteeBy ownerBy = GranteeBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String owner = eOwner.getText();
            
            addOwner(prov, group, ownerType, ownerBy, owner);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "type", ownerType.getCode(),
                            "owner", owner})); 
        }
        
        
        public static void addOwner(Provisioning prov, Group group, GranteeType granteeType, 
                Key.GranteeBy granteeBy, String grantee) throws ServiceException {
            RightCommand.grantRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), Key.TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, null,
                    UserRight.RT_ownDistList, null);
        }
    }
    
    static class RemoveOwnerHandler extends DLActionHandler {

        protected RemoveOwnerHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.removeOwner;
        }

        @Override
        void handle() throws ServiceException {
            Element eOwner = eAction.getElement(AccountConstants.E_OWNER);
            GranteeType ownerType = GranteeType.fromCode(eOwner.getAttribute(AccountConstants.A_TYPE));
            GranteeBy ownerBy = GranteeBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String owner = eOwner.getText();
            
            removeOwner(prov, group, ownerType, ownerBy, owner);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "type", ownerType.getCode(),
                            "owner", owner})); 
        }
        
        public static void removeOwner(Provisioning prov, Group group, GranteeType granteeType, 
                Key.GranteeBy granteeBy, String grantee) throws ServiceException {
            RightCommand.revokeRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), Key.TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, 
                    UserRight.RT_ownDistList, null);
        }
    }
    
    private static class AddMembersHandler extends DLActionHandler {

        protected AddMembersHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.addMembers;
        }

        @Override
        void handle() throws ServiceException {
            List<String> memberList = new LinkedList<String>();
            for (Element elem : eAction.listElements(AccountConstants.E_DLM)) {
                memberList.add(elem.getTextTrim());
            }
            
            String[] members = (String[]) memberList.toArray(new String[0]); 
            prov.addGroupMembers(group, members);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                   "name", group.getName(), "members", Arrays.deepToString(members)})); 
        }
    }
    
    private static class RemoveMembersHandler extends DLActionHandler {

        protected RemoveMembersHandler(Element eAction, Group group, Account requestedAcct) {
            super(eAction, group, requestedAcct);
        }
        
        @Override
        DLAction getAction() {
            return DLAction.removeMembers;
        }

        @Override
        void handle() throws ServiceException {
            List<String> memberList = new LinkedList<String>();
            for (Element elem : eAction.listElements(AccountConstants.E_DLM)) {
                memberList.add(elem.getTextTrim());
            }
            
            String[] members = (String[]) memberList.toArray(new String[0]); 
            prov.removeGroupMembers(group, members);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                   "name", group.getName(), "members", Arrays.deepToString(members)})); 
        }
    }
}
