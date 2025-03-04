/*
 * Author : Dr. M H B Ariyaratne
 *
 * MO(Health Information), Department of Health Services, Southern Province
 * and
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean;

import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.Privileges;
import com.divudi.ejb.ApplicationEjb;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.WebUserBean;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import com.divudi.entity.Person;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.LoginsFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.WebUserDepartmentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.WebUserPrivilegeFacade;
import com.divudi.facade.WebUserRoleFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class SessionController implements Serializable, HttpSessionListener {

    @EJB
    WebUserBean webUserBean;
    @EJB
    private WebUserDepartmentFacade webUserDepartmentFacade;
    private static final long serialVersionUID = 1L;
    WebUser loggedUser = null;
    boolean logged = false;
    boolean activated = false;
    String primeTheme;
    String defLocale;
    private List<Privileges> privilegeses;
    @Inject
    SecurityController securityController;
    Department department;
    Institution institution;
    @EJB
    private CashTransactionBean cashTransactionBean;

    public void update() {
        getFacede().edit(getLoggedUser());
        getCashTransactionBean().updateDrawers();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        if (department != null) {
            institution = department.getInstitution();
        }
        this.department = department;
    }

    public Institution getInstitution() {
        if (institution == null) {
            if (department != null) {
                institution = department.getInstitution();
            } else {
                if (loggedUser != null) {
                    if (loggedUser.getInstitution() != null) {
                        institution = loggedUser.getInstitution();
                    } else if (loggedUser.getDepartment() != null) {
                        institution = loggedUser.getDepartment().getInstitution();
                    }
                }
            }
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public WebUserBean getWebUserBean() {
        return webUserBean;
    }

    public void setWebUserBean(WebUserBean webUserBean) {
        this.webUserBean = webUserBean;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public void setSecurityController(SecurityController securityController) {
        this.securityController = securityController;
    }

    @EJB
    WebUserFacade uFacade;
    @EJB
    PersonFacade pFacade;
    @EJB
    WebUserRoleFacade rFacade;
    //
    WebUser current;
    String userName;
    String passord;
    String newPassword;
    String newPasswordConfirm;
    String newPersonName;
    String newUserName;
    String newDesignation;
    String newInstitution;
    String newPasswordHint;
    String telNo;
    String email;
    private String displayName;
    WebUserRole role;

    public Date getSystemTime() {
        return new Date();
    }

    public WebUserRole getRole() {
        return role;
    }

    public void setRole(WebUserRole role) {
        this.role = role;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private WebUserFacade getFacede() {
        return uFacade;
    }

    public String loginAction() {
        if (login()) {
            return "/index.xhtml";
        } else {
            UtilityController.addErrorMessage("Login Failure. Please try again");
            return "";
        }
    }

    private boolean login() {

        getApplicationEjb().recordAppStart();

        if (userName.trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a username");
            return false;
        }
        // password
        if (isFirstVisit()) {
            prepareFirstVisit();
            return true;
        } else {
            if (department == null) {
                UtilityController.addErrorMessage("Please select a department");
                return false;
            }
            return checkUsers();
        }
    }

    private void prepareFirstVisit() {
        WebUser user = new WebUser();
        Person person = new Person();
        person.setName(userName);
        pFacade.create(person);

        WebUserRole myRole;
        myRole = new WebUserRole();
        myRole.setName("circular_editor");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("circular_adder");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("circular_viewer");
        rFacade.create(myRole);

        myRole = new WebUserRole();
        myRole.setName("admin");
        rFacade.create(myRole);

        user.setName(getSecurityController().encrypt(userName));
        user.setWebUserPassword(getSecurityController().hash(passord));
        user.setWebUserPerson(person);
        user.setActivated(true);
        user.setRole(myRole);
        uFacade.create(user);
    }

    public String registeUser() {
        if (!userNameAvailable(newUserName)) {
            UtilityController.addErrorMessage("User name already exists. Plese enter another user name");
            return "";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not matching");
            return "";
        }

        WebUser user = new WebUser();
        Person person = new Person();
        user.setWebUserPerson(person);
        user.setRole(role);

        person.setName(newPersonName);

        pFacade.create(person);
        user.setName(getSecurityController().encrypt(newUserName));
        user.setWebUserPassword(getSecurityController().hash(newPassword));
        user.setWebUserPerson(person);
        user.setTelNo(telNo);
        user.setEmail(email);
        user.setActivated(Boolean.TRUE);

        uFacade.create(user);
        UtilityController.addSuccessMessage("New User Registered.");
        return "";
    }

    public String changePassword() {
        WebUser user = getLoggedUser();
        if (!getSecurityController().matchPassword(passord, user.getWebUserPassword())) {
            UtilityController.addErrorMessage("The old password you entered is incorrect");
            return "";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not maching");
            return "";
        }

        user.setWebUserPassword(getSecurityController().hash(newPassword));
        uFacade.edit(user);
        //
        UtilityController.addSuccessMessage("Password changed");
        return "index";
    }

    public void changeCurrentUserPassword() {
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("Select a User");
            return;
        }
        WebUser user = getCurrent();

        if (!newPassword.equals(newPasswordConfirm)) {
            UtilityController.addErrorMessage("Password and Re-entered password are not maching");
            return;
        }

        user.setWebUserPassword(getSecurityController().hash(newPassword));
        uFacade.edit(user);
        UtilityController.addSuccessMessage("Password changed");
    }

    public Boolean userNameAvailable(String userName) {
        Boolean available = true;
        List<WebUser> allUsers = getFacede().findAll();
        for (WebUser w : allUsers) {
            if (userName.toLowerCase().equals(getSecurityController().decrypt(w.getName()).toLowerCase())) {
                available = false;
            }
        }
        return available;
    }

    private boolean isFirstVisit() {
        if (getFacede().count() <= 0) {
            UtilityController.addSuccessMessage("First Visit");
            return true;
        } else {
//            UtilityController.addSuccessMessage("Welcome back");
            return false;
        }

    }

    public boolean isFirstLogin() {
        if (getFacede().count() <= 1) {
            return true;
        } else {
            return false;
        }

    }

    private boolean checkUsers() {
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false";
        List<WebUser> allUsers = getFacede().findBySQL(temSQL);
        for (WebUser u : allUsers) {
            // System.out.println("u = " + u);
            // System.out.println("u.getId() = " + u.getId());
            // System.out.println("u.getId() = " + u.getCode());
            // System.out.println("u.getName() = " + u.getName());
            // System.out.println("userName = " + userName);
            if (getSecurityController().decrypt(u.getName()).equalsIgnoreCase(userName)) {

                boolean passwordMatch = getSecurityController().matchPassword(passord, u.getWebUserPassword());

                boolean usedForTesting = false;

                if (passwordMatch || usedForTesting) {
                    if (!canLogToDept(u, department)) {
                        UtilityController.addErrorMessage("No privilage to Login This Department");
                        return false;
                    }
                    if (getApplicationController().isLogged(u) != null) {
                        UtilityController.addErrorMessage("This user already logged. Other instances will be logged out now.");
                    }

                    u.setDepartment(department);
                    u.setInstitution(institution);

                    getFacede().edit(u);

                    setLoggedUser(u);
                    setLogged(Boolean.TRUE);
                    setActivated(u.isActivated());
                    setRole(u.getRole());
                    getWebUserBean().setLoggedUser(u);

                    recordLogin();

                    UtilityController.addSuccessMessage("Logged successfully");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canLogToDept(WebUser e, Department d) {
        String sql;
        sql = "select wd from WebUserDepartment wd where wd.retired=false and wd.webUser.id=" + e.getId() + " and wd.department.id = " + d.getId();
        return !getWebUserDepartmentFacade().findBySQL(sql).isEmpty();
    }

    @Inject
    ApplicationController applicationController;

    @EJB
    ApplicationEjb applicationEjb;

    public ApplicationEjb getApplicationEjb() {
        return applicationEjb;
    }

    public void setApplicationEjb(ApplicationEjb applicationEjb) {
        this.applicationEjb = applicationEjb;
    }

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    @Inject
    private PharmacySaleController pharmacySaleController;

    public String logout() {
        userPrivilages = null;
        recordLogout();
        setLoggedUser(null);
        getWebUserBean().setLoggedUser(null);
        setLogged(false);
        setActivated(false);
        getPharmacySaleController().makeNull();
        return "/index.xhtml";

    }

    public WebUser getCurrent() {
        if (current == null) {
            current = new WebUser();
            Person p = new Person();
            current.setWebUserPerson(p);
        }
        return current;
    }

    public void setCurrent(WebUser current) {
        this.current = current;
    }

    public WebUserFacade getEjbFacade() {
        return uFacade;
    }

    public void setEjbFacade(WebUserFacade ejbFacade) {
        this.uFacade = ejbFacade;
    }

    public String getPassord() {
        return passord;
    }

    public void setPassord(String passord) {
        this.passord = passord;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNewDesignation() {
        return newDesignation;
    }

    public void setNewDesignation(String newDesignation) {
        this.newDesignation = newDesignation;
    }

    public String getNewInstitution() {
        return newInstitution;
    }

    public void setNewInstitution(String newInstitution) {
        this.newInstitution = newInstitution;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public String getNewPasswordHint() {
        return newPasswordHint;
    }

    public void setNewPasswordHint(String newPasswordHint) {
        this.newPasswordHint = newPasswordHint;
    }

    public String getNewPersonName() {
        return newPersonName;
    }

    public void setNewPersonName(String newPersonName) {
        this.newPersonName = newPersonName;
    }

    public PersonFacade getpFacade() {
        return pFacade;
    }

    public void setpFacade(PersonFacade pFacade) {
        this.pFacade = pFacade;
    }

    public WebUserFacade getuFacade() {
        return uFacade;
    }

    public void setuFacade(WebUserFacade uFacade) {
        this.uFacade = uFacade;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String newUserName) {
        this.newUserName = newUserName;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        setLogged(activated);
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public WebUserRoleFacade getrFacade() {
        return rFacade;
    }

    public void setrFacade(WebUserRoleFacade rFacade) {
        this.rFacade = rFacade;
    }

    public String getDisplayName() {
        return getSecurityController().decrypt(getLoggedUser().getName());
    }

    /**
     * Creates a new instance of SessionController
     */
    public SessionController() {
        //////// System.out.println("session started");
    }

    public String getDefLocale() {
        defLocale = "en";
        if (getLoggedUser() != null) {
            if (getLoggedUser().getDefLocale() != null) {
                if (!getLoggedUser().getDefLocale().equals("")) {
                    return getLoggedUser().getDefLocale();
                }
            }
        }
        return defLocale;
    }

    public void setDefLocale(String defLocale) {
        this.defLocale = defLocale;
    }

    public String getPrimeTheme() {
//        if (getLoggedUser() != null) {
//            if (getLoggedUser().getPrimeTheme() != null) {
//                if (!getLoggedUser().getPrimeTheme().equals("")) {
//                    return getLoggedUser().getPrimeTheme();
//                }
//            }
//        }
//        if (primeTheme == null || primeTheme.equals("")) {
//            primeTheme = "bootstrap-light-outlined";
//        }
        primeTheme = "bootstrap-light-outlined";
        return primeTheme;

    }

    public void setPrimeTheme(String primeTheme) {
        this.primeTheme = primeTheme;
    }

    /**
     *
     * @return
     */
    public WebUser getLoggedUser() {
        return loggedUser;
    }

    /**
     *
     * @param loggedUser
     */
    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
        getWebUserBean().setLoggedUser(loggedUser);
    }

    public List<Privileges> getPrivilegeses() {
        if (privilegeses == null || privilegeses.isEmpty()) {
            privilegeses.addAll(Arrays.asList(Privileges.values()));
        }
        return privilegeses;
    }
    private List<WebUserPrivilege> userPrivilages;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    public List<WebUserPrivilege> getUserPrivileges() {
        if (userPrivilages == null) {
            String sql;
            sql = "select w from WebUserPrivilege w where w.retired=false and w.webUser.id = " + getLoggedUser().getId();
            //////// System.out.println("5");
            userPrivilages = getWebUserPrivilegeFacade().findBySQL(sql);
        }
        if (userPrivilages == null) {
            userPrivilages = new ArrayList<>();
        }
        return userPrivilages;
    }

    public void setPrivilegeses(List<Privileges> privilegeses) {
        this.privilegeses = privilegeses;
    }

    public WebUserDepartmentFacade getWebUserDepartmentFacade() {
        return webUserDepartmentFacade;
    }

    public void setWebUserDepartmentFacade(WebUserDepartmentFacade webUserDepartmentFacade) {
        this.webUserDepartmentFacade = webUserDepartmentFacade;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WebUserPrivilegeFacade getWebUserPrivilegeFacade() {
        return webUserPrivilegeFacade;
    }

    public void setWebUserPrivilegeFacade(WebUserPrivilegeFacade webUserPrivilegeFacade) {
        this.webUserPrivilegeFacade = webUserPrivilegeFacade;
    }

    public void setUserPrivilages(List<WebUserPrivilege> userPrivilages) {
        this.userPrivilages = userPrivilages;
    }
    Logins thisLogin;

    public Logins getThisLogin() {
        return thisLogin;
    }

    public void setThisLogin(Logins thisLogin) {
        this.thisLogin = thisLogin;
    }
    @EJB
    LoginsFacade loginsFacade;

    public LoginsFacade getLoginsFacade() {
        return loginsFacade;
    }

    public void setLoginsFacade(LoginsFacade loginsFacade) {
        this.loginsFacade = loginsFacade;
    }

    private void recordLogin() {
        if (thisLogin != null) {
            thisLogin.setLogoutAt(Calendar.getInstance().getTime());
            if (thisLogin.getId() != null && thisLogin.getId() != 0) {
                getLoginsFacade().edit(thisLogin);
            }
        }

        thisLogin = new Logins();
        thisLogin.setLogedAt(Calendar.getInstance().getTime());
        thisLogin.setInstitution(institution);
        thisLogin.setDepartment(department);
        thisLogin.setWebUser(loggedUser);

        HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        String ipAddr = httpServletRequest.getHeader("X-FORWARDED-FOR");
        String ipAddress = (ipAddr == null) ? httpServletRequest.getRemoteAddr() : ipAddr;
        String host = httpServletRequest.getRemoteHost();

        thisLogin.setIpaddress(ipAddress);
        thisLogin.setComputerName(host);

        getLoginsFacade().create(thisLogin);
        getApplicationController().addToLoggins(this);
    }

    @PreDestroy
    private void recordLogout() {
        //////// System.out.println("session distroyed " + thisLogin);
        if (thisLogin == null) {
            return;
        }
        applicationController.removeLoggins(this);
        thisLogin.setLogoutAt(Calendar.getInstance().getTime());
        getLoginsFacade().edit(thisLogin);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        //////// System.out.println("starting session");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        //////// System.out.println("recording logout as session is distroid");
        recordLogout();
    }

    public PharmacySaleController getPharmacySaleController() {
        return pharmacySaleController;
    }

    public void setPharmacySaleController(PharmacySaleController pharmacySaleController) {
        this.pharmacySaleController = pharmacySaleController;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }
}
