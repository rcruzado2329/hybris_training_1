/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.utility;

import de.hybris.platform.servicelayer.user.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EditorAccessRights {

    private UserService userService;
    private List<String> adminRoles;


    public boolean isUserAdmin() {
        // Check what user roles are to determine if in an admin capacity, if not, remove editing permissions

        //Cannot properly confirm credential, default to regular User
        if(getAdminRoles() == null){
            return false;
        }

        final ArrayList<String> currentRoles = new ArrayList<>();
        getUserService().getCurrentUser().getGroups().forEach(role -> {
            currentRoles.add(role.getUid());
        });
        if (!Collections.disjoint(currentRoles, getAdminRoles())){
            return true; // if an admin role
        }
        else{
            return false; // if not an admin role
        }
    }

    protected List<String> getAdminRoles(){
        return adminRoles;
    }

    protected UserService getUserService(){
        return userService;
    }

    public void setUserService(final UserService userService) {
        this.userService = userService;
    }

    public void setAdminRoles(final List<String> adminRolesList){
        this.adminRoles = adminRolesList;
    }
}
