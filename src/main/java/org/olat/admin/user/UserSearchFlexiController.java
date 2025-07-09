/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.admin.user;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityPowerSearchQueries;
import org.olat.basesecurity.OrganisationRoles;
import org.olat.basesecurity.OrganisationService;
import org.olat.basesecurity.SearchIdentityParams;
import org.olat.basesecurity.events.MultiIdentityChosenEvent;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.*;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.AutoCompleteFormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.SingleSelectionImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.util.OrganisationUIFactory;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListProvider;
import org.olat.core.gui.control.winmgr.CommandFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Organisation;
import org.olat.core.id.Roles;
import org.olat.core.id.UserConstants;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.modules.curriculum.*;
import org.olat.modules.curriculum.model.CurriculumSearchParameters;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryStatusEnum;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.olat.user.AbstractUserPropertyHandler;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.EmailProperty;
import org.olat.user.propertyhandlers.GenericSelectionPropertyHandler;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Initial Date:  Jul 29, 2003
 *
 * 
 * Comment:  
 * Subworkflow that allows the user to search for a user and choose the user from 
 * the list of users that match the search criteria. Users can be searched by
 * <ul>
 *  <li>Username
 *  <li>First name
 *  <li>Last name
 *  <li>Email address
 * </ul>
 * 
 * 
 * Events:<br>
 *         Fires a SingleIdentityChoosenEvent when an identity has been chosen
 *         which contains the choosen identity<br>
 *         Fires a MultiIdentityChoosenEvent when multiples identities have been
 *         chosen which contains the choosen identities<br>
 *         <p>
 *         Optionally set the useMultiSelect boolean to true which allows to
 *         select multiple identities from within the search results.
 *         
 *         
 * @author Felix Jost, Florian Gnaegi
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class UserSearchFlexiController extends FormBasicController {

	private static final String usageIdentifyer = UserTableDataModel.class.getCanonicalName();
	
	private FormLink backLink;
	private FormLink searchButton;
	private FormLink selectUsersButton;
	private TextElement loginEl;
	private AutoCompleter completerEl;
	private MultiSelectionFilterElement organisations;
	private MultiSelectionFilterElement courses;
	private MultiSelectionFilterElement finished;
	private MultiSelectionFilterElement curriculas;
	private Map <String,FormItem> propFormItems;
	private Map<String, FormItem> searchVariantForItem;
	private FlexiTableElement tableEl;
	private UserSearchFlexiTableModel userTableModel;

	private boolean showTable;
	private boolean showSelectUsersButton;
	private boolean multiSelection;
	private boolean isAdministrativeUser;
	private List<UserPropertyHandler> userSearchFormPropertyHandlers;
	private List<Organisation> searchableOrganisations;
	private List<RepositoryEntry> searchableCourses;
	private List<CurriculumElement> curriculsy;
	private TextElement fu;

	private UserSearchProvider search;

	@Autowired
	private UserManager userManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private BaseSecurityModule securityModule;
	@Autowired
	private OrganisationService organisationService;
	@Autowired
	private IdentityPowerSearchQueries identitySearchQueries;
	@Autowired
	private RepositoryEntryDAO rDao;
	@Autowired
	private CurriculumService curriculumService;

	public UserSearchFlexiController(UserRequest ureq, WindowControl wControl, Form rootForm) {
		this(ureq, wControl, rootForm, null, null, true, false);
	}
	
	public UserSearchFlexiController(UserRequest ureq, WindowControl wControl, Form rootForm,
			GroupRoles repositoryEntryRole, OrganisationRoles[] excludedRoles,
			boolean multiSelection, boolean showSelectButton) {
		this(ureq, wControl, rootForm, repositoryEntryRole, excludedRoles, multiSelection, showSelectButton, true);
	}
	
	public UserSearchFlexiController(UserRequest ureq, WindowControl wControl, Form rootForm,
			GroupRoles repositoryEntryRole, OrganisationRoles[] excludedRoles,
			boolean multiSelection, boolean showSelectButton, boolean showTable) {
		super(ureq, wControl, LAYOUT_CUSTOM, "usersearchext", rootForm);
		
		init(ureq, null, repositoryEntryRole, excludedRoles, multiSelection, showSelectButton, showTable);

		initForm(ureq);
	}
	
	public UserSearchFlexiController(UserRequest ureq, WindowControl wControl, GroupRoles repositoryEntryRole, boolean multiSelection) {
		super(ureq, wControl, "usersearchext");
		
		init(ureq, null, repositoryEntryRole, null, multiSelection, true, true);
		
		initForm(ureq);
	}
	
	public UserSearchFlexiController(UserRequest ureq, WindowControl wControl, UserSearchProvider searchProvider, boolean multiSelection) {
		super(ureq, wControl, "usersearchext");
		
		init(ureq, searchProvider, null, null, multiSelection, true, true);

		initForm(ureq);
	}
	
	private void init(UserRequest ureq, UserSearchProvider searchProvider, GroupRoles repositoryEntryRole,
			OrganisationRoles[] excludedRoles, boolean multiSelect, boolean showSelectButton, boolean showTable) {
		setTranslator(Util.createPackageTranslator(UserPropertyHandler.class, getLocale(), getTranslator()));
		setTranslator(Util.createPackageTranslator(UserSearchFlexiController.class, getLocale(), getTranslator()));

		this.showTable = showTable;
		this.multiSelection = multiSelect;
		
		Roles roles = ureq.getUserSession().getRoles();
		isAdministrativeUser = securityModule.isUserAllowedAdminProps(roles);
		List<UserPropertyHandler> allSearchFormPropertyHandlers = userManager.getUserPropertyHandlersFor(UserSearchForm.class.getCanonicalName(), isAdministrativeUser);
		userSearchFormPropertyHandlers = allSearchFormPropertyHandlers.stream()
				.filter(prop -> !UserConstants.NICKNAME.equals(prop.getName()))// admin. has the login search field
				.collect(Collectors.toList());
		
		if(searchProvider != null) {
			search = searchProvider;
		} else {
			searchableOrganisations = organisationService.getOrganisations(getIdentity(), roles,
					OrganisationRoles.valuesWithoutGuestAndInvitee());
			search = new UserSearchQueries(searchableOrganisations, repositoryEntryRole, excludedRoles);
		}
		
		this.showSelectUsersButton = showSelectButton;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;

			// insert a autocompleter search
			Roles roles = ureq.getUserSession().getRoles();
			boolean autofocus = true;
			boolean autoCompleteAllowed = securityModule.isUserAllowedAutoComplete(roles);
			if(autoCompleteAllowed) {
				FormLayoutContainer quickSearchFormContainer = FormLayoutContainer.createDefaultFormLayout("quicksearchPanel", getTranslator());
				quickSearchFormContainer.setFormTitle(translate("quick.search.title"));
				quickSearchFormContainer.setRootForm(mainForm);
				layoutCont.add("quicksearchPanel", quickSearchFormContainer);

				completerEl = uifactory.addTextElementWithAutoCompleter("quick.search", "quick.search", 64, null, quickSearchFormContainer);
				completerEl.setListProvider(search, ureq.getUserSession());
				completerEl.setExampleKey("quick.search.help", null);
				completerEl.setMinLength(3);
				completerEl.setShowDisplayKey(isAdministrativeUser);
				completerEl.setFocus(true);
				autofocus = false;
			}

			// user search form
			backLink = uifactory.addFormLink("btn.back", formLayout);
			backLink.setIconLeftCSS("o_icon o_icon_back");

			FormLayoutContainer searchFormContainer = FormLayoutContainer.createDefaultFormLayout("usersearchPanel", getTranslator());
			searchFormContainer.setRootForm(mainForm);
			searchFormContainer.setElementCssClass("o_sel_usersearch_searchform");
			searchFormContainer.setFormTitle(translate("header.normal"));
			layoutCont.add(searchFormContainer);
			layoutCont.add("usersearchPanel", searchFormContainer);

			propFormItems = new HashMap<>();
			searchVariantForItem = new HashMap<>();

			AtomicInteger lid = new AtomicInteger(1);
			FormItemContainer pcl = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()),"search.form.login", searchFormContainer);
			FormItem sdl = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pcl, AbstractUserPropertyHandler.optsValuesFull, AbstractUserPropertyHandler.selectOptsFull);
			sdl.setElementCssClass("col-sm-2");
			loginEl = uifactory.addTextElement("login", "", 128, "", pcl);
			loginEl.setVisible(isAdministrativeUser);
			if(autofocus && loginEl.isVisible()) {
				loginEl.setFocus(true);
				autofocus = false;
			}

			searchVariantForItem.put("login", sdl);

			// dealer dropdown
			FormItemContainer pco = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()),"search.form.title.organisations", searchFormContainer);
			pco.setElementCssClass("o_form_cell");
			FormItem sdo = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pco, AbstractUserPropertyHandler.optsValuesCnc, AbstractUserPropertyHandler.selectOptsCnc);
			sdo.setElementCssClass("col-sm-2");
			organisations = uifactory.addCheckboxesFilterDropdown("organisations", "",
					pco, getWindowControl(), OrganisationUIFactory.createSelectionValues(organisationService.getOrganisations(getIdentity(), roles,
							OrganisationRoles.valuesWithoutGuestAndInvitee()), getLocale()));
			organisations.setElementCssClass("col-sm-6");

			//propFormItems.put(organisations.getName(), organisations);
			searchVariantForItem.put(organisations.getName(), sdo);

			// course participant dropdown
			FormItemContainer pcp = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()),"search.form.title.entrolled", searchFormContainer);
			pcp.setElementCssClass("o_form_cell");
			FormItem sdp = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pcp, AbstractUserPropertyHandler.optsValuesCnc, AbstractUserPropertyHandler.selectOptsCnc);
			sdp.setElementCssClass("col-sm-2");
			searchableCourses = rDao.loadForMe(RepositoryEntryStatusEnum.published);
			courses = uifactory.addCheckboxesFilterDropdown("courses", "", pcp, getWindowControl(),
					OrganisationUIFactory.createCoursesSelectionValues(searchableCourses));
			courses.setElementCssClass("col-sm-6");

			searchVariantForItem.put(courses.getName(), sdp);

			// course finished dropdown
			FormItemContainer pcf = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()),"search.form.title.course.finished", searchFormContainer);
			pcf.setElementCssClass("o_form_cell");
			FormItem sdf = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pcf, AbstractUserPropertyHandler.optsValuesCnc, AbstractUserPropertyHandler.selectOptsCnc);
			sdf.setElementCssClass("col-sm-2");
			finished = uifactory.addCheckboxesFilterDropdown("finished", "", pcf, getWindowControl(),
					OrganisationUIFactory.createCoursesSelectionValues(searchableCourses));
			finished.setElementCssClass("col-sm-6");

			searchVariantForItem.put(finished.getName(), sdf);

			// ścieżki kariery dropdown
			FormItemContainer pcs = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()),"search.form.title.course.learnpath", searchFormContainer);
			pcs.setElementCssClass("o_form_cell");
			FormItem sds = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pcs, AbstractUserPropertyHandler.optsValuesCnc, AbstractUserPropertyHandler.selectOptsCnc);
			sds.setElementCssClass("col-sm-2");
			CurriculumSearchParameters params = new CurriculumSearchParameters();
			Collection<? extends CurriculumRef> l = curriculumService.getCurriculums(params).stream().map(e -> (CurriculumRef) e).collect(Collectors.toList());
			curriculsy = new ArrayList<>();
			Map<String, Collection<CurriculumElement>> map = new HashMap<>();
			l.forEach(r -> {
				Collection<CurriculumElement> c = curriculumService.getCurriculumElements(r, new CurriculumElementStatus[]{CurriculumElementStatus.active});
				curriculsy.addAll(c);
				map.put(((Curriculum) r).getDisplayName(), c);
			});
			curriculas = uifactory.addCheckboxesFilterDropdown("learnpaths", "", pcs, getWindowControl(),
					OrganisationUIFactory.createCurriculasValues(map));
			curriculas.setElementCssClass("col-sm-6");

			searchVariantForItem.put(curriculas.getName(), sds);

			for (UserPropertyHandler userPropertyHandler : userSearchFormPropertyHandlers) {
				if (userPropertyHandler == null) continue;

				FormItemContainer pc = uifactory.addHorizontalFormLayout(String.valueOf(lid.getAndIncrement()), userPropertyHandler.i18nFormElementLabelKey(), searchFormContainer);
				pc.setElementCssClass("o_form_cell");
				FormItem ssd;
//				if(userPropertyHandler instanceof GenericSelectionPropertyHandler) {
//					ssd = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pc, AbstractUserPropertyHandler.optsValuesCnc, AbstractUserPropertyHandler.selectOptsCnc);
//				} else {
					ssd = uifactory.addDropdownSingleselect(String.valueOf(lid.getAndIncrement()), "", pc, AbstractUserPropertyHandler.optsValuesFull, AbstractUserPropertyHandler.selectOptsFull);
//				}
				ssd.setElementCssClass("col-sm-2");
				FormItem fi = userPropertyHandler.addFormItem(getLocale(), null, UserSearchForm.class.getCanonicalName(), false, pc);
				fi.setLabel("search.form.title.empty", null);
				if(autofocus && fi instanceof TextElement te) {
					te.setFocus(true);
					autofocus = false;
				}
				
				// DO NOT validate email field => see OLAT-3324, OO-155, OO-222
				if (userPropertyHandler instanceof EmailProperty && fi instanceof TextElement textElement) {
					textElement.setItemValidatorProvider(null);
				} else if(fi instanceof SingleSelection selection) {
					selection.setAllowNoSelection(true);
				}

				propFormItems.put(userPropertyHandler.getName(), fi);
				searchVariantForItem.put(userPropertyHandler.getName(), ssd);
			}
			
			FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
			buttonGroupLayout.setRootForm(mainForm);
			searchFormContainer.add(buttonGroupLayout);
			// Don't use submit button, form should not be marked as dirty since this is
			// not a configuration form but only a search form (OLAT-5626)
			searchButton = uifactory.addFormLink("submit.search", buttonGroupLayout, Link.BUTTON);

			layoutCont.contextPut("noList","false");		
			layoutCont.contextPut("showButton","false");

			//add the table
			FlexiTableColumnModel tableColumnModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
			int colPos = 0;
			List<UserPropertyHandler> userPropertyHandlers = userManager.getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
			List<UserPropertyHandler> resultingPropertyHandlers = new ArrayList<>();
			// followed by the users fields
			for (int i = 0; i < userPropertyHandlers.size(); i++) {
				UserPropertyHandler userPropertyHandler	= userPropertyHandlers.get(i);
				boolean visible = UserManager.getInstance().isMandatoryUserProperty(usageIdentifyer , userPropertyHandler);
				if(visible) {
					resultingPropertyHandlers.add(userPropertyHandler);
					tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(userPropertyHandler.i18nColumnDescriptorLabelKey(), colPos++, true, userPropertyHandler.getName()));
				}
			}
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("select", translate("select"), "select"));
			
			Translator myTrans = userManager.getPropertyHandlerTranslator(getTranslator());
			userTableModel = new UserSearchFlexiTableModel(Collections.<Identity>emptyList(), resultingPropertyHandlers, getLocale(), tableColumnModel);
			tableEl = uifactory.addTableElement(getWindowControl(), "users", userTableModel, 250, false, myTrans, formLayout);
			tableEl.setCustomizeColumns(false);
			tableEl.setMultiSelect(multiSelection);
			tableEl.setSelectAllEnable(multiSelection);
			tableEl.setVisible(false);
			
			if (showSelectUsersButton) {
				selectUsersButton = uifactory.addFormLink("select", formLayout);
				tableEl.addBatchButton(selectUsersButton);
			}

			layoutCont.put("userTable", tableEl.getComponent());
		}
	}

	protected List<Organisation> getOrganisations() {
		List<Organisation> selectedOrganisations = new ArrayList<>();
		Collection<String> selectedKeys = organisations.getSelectedKeys();
		for(Organisation organisation : searchableOrganisations) {
			if(selectedKeys.contains(organisation.getKey().toString())) {
				selectedOrganisations.add(organisation);
			}
		}
		return selectedOrganisations;
	}

	protected List<RepositoryEntry> getCourses() {
		List<RepositoryEntry> selectedCourses = new ArrayList<>();
		Collection<String> selectedKeys = courses.getSelectedKeys();
		for(RepositoryEntry course : searchableCourses) {
			if(selectedKeys.contains(course.getKey().toString())) {
				selectedCourses.add(course);
			}
		}
		return selectedCourses;
	}

	protected List<RepositoryEntry> getFinished() {
		List<RepositoryEntry> selectedCourses = new ArrayList<>();
		Collection<String> selectedKeys = finished.getSelectedKeys();
		for(RepositoryEntry course : searchableCourses) {
			if(selectedKeys.contains(course.getKey().toString())) {
				selectedCourses.add(course);
			}
		}
		return selectedCourses;
	}

	protected List<CurriculumElement> getCur() {
		List<CurriculumElement> selectedCurriculums = new ArrayList<>();
		Collection<String> selectedKeys = curriculas.getSelectedKeys();
		for(CurriculumElement c: curriculsy) {
			if(selectedKeys.contains(c.getKey().toString())) {
				selectedCurriculums.add(c);
			}
		}
		return selectedCurriculums;
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == backLink) {
			flc.contextPut("showButton","false");
		} else {
			super.event(ureq, source, event);
		}
	}
	
	//@Override
	protected void doFireSelection(UserRequest ureq, List<String> res) {
		String mySel = res.isEmpty() ? null : res.get(0);
		if(StringHelper.containsNonWhitespace(mySel) && StringHelper.isLong(mySel)) {
			try {
				Long key = Long.valueOf(mySel);				
				if (key > 0) {
					Identity chosenIdent = securityManager.loadIdentityByKey(key);
					if(chosenIdent != null) {
						fireEvent(ureq, new SingleIdentityChosenEvent(chosenIdent));
						List<Identity> selectedIdentities = Collections.singletonList(chosenIdent);
						userTableModel.setObjects(selectedIdentities);
						Set<Integer> selectedIndex = new HashSet<>();
						selectedIndex.add(Integer.valueOf(0));
						tableEl.setMultiSelectedIndex(selectedIndex);
						tableEl.setVisible(true);
					}
				}
			} catch (NumberFormatException e) {
				getWindowControl().setWarning(translate("error.no.user.found"));
			}
		} else {
			getWindowControl().setWarning(translate("error.search.form.notempty"));
		}
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		return true;
	}
	
	private boolean validateForm(UserRequest ureq) {
		// override for sys admins
		UserSession usess = ureq.getUserSession();
		if (usess != null && usess.getRoles() != null
				&& (usess.getRoles().isAdministrator() || usess.getRoles().isRolesManager())) {
			return true;
		}
		
		boolean filled = !loginEl.isEmpty();
		boolean filledSelection = false;
		StringBuilder  full = new StringBuilder(loginEl.getValue().trim());
		FormItem lastFormElement = loginEl;
		
		// DO NOT validate each user field => see OLAT-3324
		// this are custom fields in a Search Form
		// the same validation logic can not be applied
		// i.e. email must be searchable and not about getting an error like
		// "this e-mail exists already"
		for (UserPropertyHandler userPropertyHandler : userSearchFormPropertyHandlers) {
			FormItem ui = propFormItems.get(userPropertyHandler.getName());
			ui.clearError();
			
			String uiValue = userPropertyHandler.getStringValue(ui);
			// add value for later non-empty search check
			if (StringHelper.containsNonWhitespace(uiValue)) {
				full.append(uiValue.trim());
				filled = true;
			}
			if(ui instanceof SingleSelection uiSelection) {
				filledSelection |= uiSelection.isOneSelected()
						&& !GenericSelectionPropertyHandler.NO_SEL_KEY.equals(uiSelection.getSelectedKey());
			} else if(ui instanceof MultipleSelectionElement uiSelection) {
				filledSelection |= uiSelection.isAtLeastSelected(1);
			}

			lastFormElement = ui;
		}

		// Don't allow searches with * or %  or @ chars only (wild cards). We don't want
		// users to get a complete list of all OLAT users this easily.
		String fullString = full.toString();
		boolean onlyStar= fullString.matches("^[\\*\\s@\\%]*$");

		if ((!filled && !filledSelection) || onlyStar) {
			// set the error message
			lastFormElement.setErrorKey("error.search.form.notempty");
			return false;
		}
		if (fullString.contains("**") ) {
			lastFormElement.setErrorKey("error.search.form.no.wildcard.dublicates");
			return false;
		}		
		if (fullString.length() < 4 && !filledSelection) {
			lastFormElement.setErrorKey("error.search.form.to.short");
			return false;
		}
		return true;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == backLink) {
			flc.contextPut("noList","false");			
			flc.contextPut("showButton","false");
			if(userTableModel != null) {
				userTableModel.setObjects(new ArrayList<>());
				tableEl.reset();
				tableEl.setVisible(false);
			}
		} else if(searchButton == source) {
			if(validateForm(ureq)) {
				getWindowControl().getWindowBackOffice().sendCommandTo(CommandFactory.createScrollTop());
				doSearch(ureq);
			}
		} else if(source == completerEl) {
			if(event instanceof AutoCompleteFormEvent acfe
					&& StringHelper.containsNonWhitespace(acfe.getKey())) {
				doSelect(ureq, acfe.getKey());
			}
		} else if (source == selectUsersButton) {
			fireEvent(ureq, new MultiIdentityChosenEvent(userTableModel.getObjects(tableEl.getMultiSelectedIndex())));
		} else if (tableEl == source) {
			if(event instanceof SelectionEvent se && "select".equals(se.getCommand())) {
				Identity chosenIdent = userTableModel.getObject(se.getIndex());
				fireEvent(ureq, new SingleIdentityChosenEvent(chosenIdent));
			}
		} else {
			super.formInnerEvent(ureq, source, event);
		}
	}

	@Override
	protected void formNext(UserRequest ureq) {
		//
	}

	@Override
	protected void formFinish(UserRequest ureq) {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		String searchValue = getAutoCompleterSearchValue();
		if(StringHelper.containsNonWhitespace(searchValue) && !hasSearchProperties()) {
			doSelect(ureq, searchValue);
		} else if(validateForm(ureq)) {
			doSearch(ureq);
		}
	}
	
	private String getAutoCompleterSearchValue() {
		if(completerEl == null || !completerEl.isVisible()) return null;
		
		String searchValue = completerEl.getKey();
		if(!StringHelper.containsNonWhitespace(searchValue)) {
			searchValue = completerEl.getValue();
		}
		return searchValue;
	}
	
	public List<Identity> getSelectedIdentities() {
		Set<Integer> index = tableEl.getMultiSelectedIndex();		
		List<Identity> selectedIdentities =	new ArrayList<>(index.size());
		for(Integer i : index) {
			Identity selectedIdentity = userTableModel.getObject(i.intValue());
			selectedIdentities.add(selectedIdentity);
		}
		return selectedIdentities;
	}
	
	private boolean hasSearchProperties() {
		return StringHelper.containsNonWhitespace(loginEl.getValue())
				|| collectSearchProperties() != null;
	}
	
	private void doSelect(UserRequest ureq, String searchValue) {
		if(StringHelper.containsNonWhitespace(searchValue)) {
			if(StringHelper.isLong(searchValue)) {
				doFireSelection(ureq, Collections.singletonList(searchValue));
			} else if(searchValue.length() >= 3){
				Map<String, String> userProperties = new HashMap<>();
				userProperties.put(UserConstants.FIRSTNAME, searchValue);
				userProperties.put(UserConstants.LASTNAME, searchValue);
				userProperties.put(UserConstants.EMAIL, searchValue);
				List<Identity> res = search.searchUsers(searchValue, userProperties, false);
				if(res.size() == 1) {
					//do select
					Identity chosenIdent = res.get(0);
					fireEvent(ureq, new SingleIdentityChosenEvent(chosenIdent));
				} else if (res.size() > 1){
					tableEl.reset();
					tableEl.setVisible(true);
					userTableModel.setObjects(res);
					flc.contextPut("showButton","true");
				}
			}
		}
	}
	
	public void doSearch(UserRequest ureq) {
		String login = loginEl.getValue();
		Map<String, String> userPropertiesSearch = collectSearchProperties();
		List<Identity> users;
		if(search instanceof RichUserSearchProvider) {
			users = ((RichUserSearchProvider) search).searchUsers(login, userPropertiesSearch, collectSearchVariants(), true);
		} else {
			users = search.searchUsers(login, userPropertiesSearch, true);
		}
		
		if(showTable) {
			tableEl.setVisible(true);
			tableEl.setEmptyTableMessageKey("error.no.user.found");
			userTableModel.setObjects(users);
			tableEl.reset(true, true, true);
			flc.contextPut("showButton","true");
		} else {
			fireEvent(ureq, new MultiIdentityChosenEvent(users));
		}
	}
	
	private Map<String, String> collectSearchProperties() {
		// build user fields search map
		Map<String, String> userPropertiesSearch = new HashMap<>();				
		for (UserPropertyHandler userPropertyHandler : userSearchFormPropertyHandlers) {
			if (userPropertyHandler == null) continue;
			
			FormItem ui = propFormItems.get(userPropertyHandler.getName());
			String uiValue = userPropertyHandler.getStringValue(ui);
			if(userPropertyHandler.getName().startsWith("genericCheckboxProperty")) {
				if(!"false".equals(uiValue)) {
					userPropertiesSearch.put(userPropertyHandler.getName(), uiValue);
				}
			} else if (StringHelper.containsNonWhitespace(uiValue)) {
				userPropertiesSearch.put(userPropertyHandler.getName(), uiValue);
			}
		}
		if (userPropertiesSearch.isEmpty()) {
			userPropertiesSearch = null;
		}
		return userPropertiesSearch;
	}

	private Map<String, String> collectSearchVariants() {
		Map<String, String> searchVariants = new HashMap<>();
		for (UserPropertyHandler userPropertyHandler : userSearchFormPropertyHandlers) {
			if (userPropertyHandler == null) continue;
			FormItem varItem = searchVariantForItem.get(userPropertyHandler.getName());
			String uiVariant = ((SingleSelection) varItem).getSelectedKey();
			searchVariants.put(userPropertyHandler.getName(), uiVariant);
		}
		searchVariants.put("organisations",
				((SingleSelection) searchVariantForItem.get("organisations")).getSelectedKey());
		searchVariants.put("courses",
				((SingleSelection) searchVariantForItem.get("courses")).getSelectedKey());
		searchVariants.put("finished",
				((SingleSelection) searchVariantForItem.get("finished")).getSelectedKey());
		searchVariants.put("learnpaths",
				((SingleSelection) searchVariantForItem.get("learnpaths")).getSelectedKey());
/*		searchVariants.put("orgunit",
				((SingleSelection) searchVariantForItem.get("orgunit")).getSelectedKey());*/

		return searchVariants;
	}

	private class UserSearchQueries extends UserSearchListProvider implements RichUserSearchProvider {
		
		public UserSearchQueries(List<Organisation> searchableOrganisations, GroupRoles repositoryEntryRole,
				OrganisationRoles[] excludedRoles) {
			super(searchableOrganisations, repositoryEntryRole, excludedRoles);
		}
		
		/**
		 * Can be overwritten by subclassen to search other users or filter users.
		 * @param login
		 * @param userPropertiesSearch
		 * @return
		 */
		@Override
		public List<Identity> searchUsers(String login, Map<String, String> userPropertiesSearch, Map<String, String> searchVariants, boolean userPropertiesAsIntersectionSearch) {
			SearchIdentityParams params = new SearchIdentityParams(login,
					userPropertiesSearch, searchVariants, userPropertiesAsIntersectionSearch, null, null,
					null, null, null, null, null, Identity.STATUS_VISIBLE_LIMIT);
			params.setOrganisationParents(getOrganisations());
			params.setCourses(getCourses());
			params.setFinished(getFinished());
			params.setCurriculums(getCur());
//			params.setOrgunit(fu.getValue());
			//params.setRepositoryEntryRole(getRepositoryEntryRole(), false);
			//params.setExcludedRoles(getExcludedRoles());
			return identitySearchQueries.getIdentitiesByPowerSearch(params, 0, -1);
		}

		@Override
		public List<Identity> searchUsers(String login, Map<String, String> userPropertiesSearch, boolean userPropertiesAsIntersectionSearch) {
			SearchIdentityParams params = new SearchIdentityParams(login,
					userPropertiesSearch, userPropertiesAsIntersectionSearch, null, null,
					null, null, null, null, null, Identity.STATUS_VISIBLE_LIMIT);
			params.setOrganisations(getSearchableOrganisations());
			params.setRepositoryEntryRole(getRepositoryEntryRole(), false);
			params.setExcludedRoles(getExcludedRoles());
			return identitySearchQueries.getIdentitiesByPowerSearch(params, 0, -1);
		}
	}
	
	public interface UserSearchProvider extends ListProvider {
		
		public List<Identity> searchUsers(String login, Map<String, String> userPropertiesSearch, boolean userPropertiesAsIntersectionSearch);
	}

	public interface RichUserSearchProvider extends UserSearchProvider {

		List<Identity> searchUsers(String login, Map<String, String> userPropertiesSearch, Map<String, String> variantSearch, boolean userPropertiesAsIntersectionSearch);
	}
}