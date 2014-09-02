package ru.bov.remark;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.LayoutEvents;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ClassResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomTable.CellStyleGenerator;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import net.sf.jasperreports.engine.JRException;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.tepi.filtertable.FilterTable;
import org.vaadin.peter.contextmenu.ContextMenu;
import ru.bov.remark.datamodel.entity.*;
import ru.bov.remark.datamodel.repository.*;
import ru.bov.remark.ui.*;
import ru.bov.remark.ui.ExpertEditor.ExpertEditorSavedEvent;
import ru.bov.remark.ui.ExpertEditor.ExpertEditorSavedListener;
import ru.bov.remark.ui.ProjectEditor.EditorSavedEvent;
import ru.bov.remark.ui.ProjectEditor.EditorSavedListener;
import ru.bov.remark.ui.RebukeEditor.RebukeEditListener;
import ru.bov.remark.ui.SelectHand.SelectHandEvent;
import ru.bov.remark.ui.SelectHand.SelectHandListener;
import ru.bov.remark.uicomponents.messagebox.ButtonId;
import ru.bov.remark.uicomponents.messagebox.MessageBoxListener;
import ru.bov.remark.utils.*;
import ru.bov.remark.utils.SetRaiseRebuke.SetRaiseRebukeEvent;
import ru.bov.remark.utils.SetRaiseRebuke.SetRaiseRebukeListener;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@org.springframework.stereotype.Component
@Scope("prototype")
public class MainPage extends CssLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    private ImportFromXML importXML;
    @Autowired
    private ExportToXML exportXML;

    @Value("#{appProps['app.lang']}")
    private String appLanguage; // язык системы

    @Value("#{appProps['check.enable.msg']}")
    private String checkEnable; // включение проверки

    @Autowired
    AnswerToRebukeReport mainReport;

    private ContextMenu projectContextMenu = new ContextMenu();
    ContextMenu.ContextMenuItem projectMenuItemCreate;
    ContextMenu.ContextMenuItem projectMenuItemEdit;
    ContextMenu.ContextMenuItem projectMenuItemDelete;
    ContextMenu.ContextMenuItem projectMenuItemReport;
    ContextMenu.ContextMenuItem projectMenuItemRefresh;
    ContextMenu.ContextMenuItem projectMenuItemClearFilter;

    private ContextMenu expertContextMenu = new ContextMenu();
    ContextMenu.ContextMenuItem expertMenuItemCreate;
    ContextMenu.ContextMenuItem expertMenuItemEdit;
    ContextMenu.ContextMenuItem expertMenuItemDelete;
    ContextMenu.ContextMenuItem expertMenuItemTaskGip;
    ContextMenu.ContextMenuItem expertMenuItemSendAll;
    ContextMenu.ContextMenuItem expertMenuItemCopy;
    ContextMenu.ContextMenuItem expertMenuItemImportXML;
    ContextMenu.ContextMenuItem expertMenuItemExportXML;

    private ContextMenu rebukeContextMenu = new ContextMenu();
    ContextMenu.ContextMenuItem rebukeMenuItemCreate;
    ContextMenu.ContextMenuItem rebukeMenuItemCreateCopy;
    ContextMenu.ContextMenuItem rebukeMenuItemEdit;
    ContextMenu.ContextMenuItem rebukeMenuItemDelete;
    ContextMenu.ContextMenuItem rebukeMenuItemAppointExecutive;
    ContextMenu.ContextMenuItem rebukeMenuItemDoneAnswer;
    ContextMenu.ContextMenuItem rebukeMenuItemRaisRebuke;
    ContextMenu.ContextMenuItem rebukeMenuItemSetRepeat;
    ContextMenu.ContextMenuItem rebukeMenuItemClearFilter;
    ContextMenu.ContextMenuItem rebukeMenuItemselectAll;
    ContextMenu.ContextMenuItem rebukeMenuItemPrint;

    private ContextMenu answerContextMenu = new ContextMenu();
    ContextMenu.ContextMenuItem answerMenuItemEdit;
    ContextMenu.ContextMenuItem answerMenuItemDelete;
    ContextMenu.ContextMenuItem answerMenuItemToTotal;
    ContextMenu.ContextMenuItem answerMenuItemFromTotal;
    ContextMenu.ContextMenuItem answerMenuItemDone;
    ContextMenu.ContextMenuItem answerMenuItemClose;
    ContextMenu.ContextMenuItem answerMenuItemOpen;

    private String user;
    private String role;
    private String department;
    private Boolean isRebukeExpand = false;
    private final Label projectStats = new Label();
    private final Label expertStats = new Label();
    private final Label rebukeStats = new Label();
    private final Label answerStats = new Label();

    private final FilterTable projectTable = new FilterTable();
    private final FilterTable expertTable = new FilterTable();
    private final FilterTable rebukeTable = new FilterTable();
    private final FilterTable answerTable = new FilterTable();
    private BeanItemContainer<Project> projects;
    private BeanItemContainer<Expert> experts;
    private BeanItemContainer<Rebuke> rebukes;
    private BeanItemContainer<Answer> answers;
    private Project projectFilter;
    private Expert expertFilter;
    private static final Set<Rebuke> rebukeFilter = new LinkedHashSet<>();

    //    @Autowired
//    private MainMenu menu;
    @Autowired
    private MenuToolBar toolBar;
    @Autowired
    private SelectHand selectHand;
    @Autowired
    private ProjectRepo projectRepo;
    @Autowired
    private ExpertRepo expertRepo;
    @Autowired
    private RebukeRepo rebukeRepo;
    @Autowired
    private AnswerRepo answerRepo;
    @Autowired
    private DepartmentRepo depRepo;
    @Autowired
    private SubcontractorsRepo subRepo;
    @Autowired
    private FilestorageRepo filestorageRepo;
    @Autowired
    private Auditing auditing;
    @Autowired
    private AnswerEditor answerEditor;
    @Autowired
    private AnswerEditorTotal answerEditorTotal;
    @Autowired
    private ExpertEditor expertEditor;
    @Autowired
    private CopyExpert copyExpertWindow;
    @Autowired
    private ProjectEditor projectEditor;
    @Autowired
    private RebukeEditor rebukeEditor;
    @Autowired
    private SetRaiseRebuke setRaiseRebuke;
    @Autowired
    private SetRepeatWindow repeatWindow;
    @Autowired
    private TaskGipWindow taskGipWindow;
    @Autowired
    private SendMailAll sendMailAll;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMMM yyyy");
    private final ClassResource ICON_TRUE = new ClassResource("/images/circle.png");
    private final ThemeResource ICON_WHITE = new ThemeResource("img/icon/white.gif");
    private final ClassResource ICON_LR = new ClassResource("/images/PNG Files/16x16/activity.png");
    private final ClassResource ICON_LOCK = new ClassResource("/images/PNG Files/16x16/locked.png");
    private final ClassResource ICON_STOPWORK = new ClassResource("/images/stop_work.png");
    private final ClassResource ICON_BACKWARD = new ClassResource("/images/left.png");
    private final ClassResource ICON_FORWARD = new ClassResource("/images/right.png");

    private static String STR_INWORKS = "Ведутся работы";
    private static String STR_ENDWORKS = "Работы завершены";
    private static String STR_SENDWORKS = "Результаты работы переданы";
    private static String NOTIFY_NOTRIGHT = "У Вас нет прав для выполнения этого действия";
    private static String SEPARATOR_LINE = "&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97&#x97";
    private static String STR_NOTEACCEPTED = "Замечание принимается";
    private static String STR_NOT_NOTEACCEPTED = "Замечание не принимается";
    private static String STR_NOTEACCEPTED_PARTIAL = "Замечание принимается частично";

    private Set<Rebuke> rebukeSet = new HashSet<>();
    private String currentDepartmentRebuke = "";
    private ComboBox depComboBox = new ComboBox();
    private ComboBox subComboBox = new ComboBox();
    private Button collapseButton = new Button();

    private Project getProjectFilter() {
        return projectFilter;
    }


    void setProjectFilter(Project projectFilter) {
        this.projectFilter = projectFilter;
        if (UI.getCurrent() != null) {
            UI.getCurrent().getSession()
                    .setAttribute("currentProject", projectFilter);
        }
    }

    Expert getExpertFilter() {
        if (UI.getCurrent() != null) {
            return (Expert) UI.getCurrent().getSession().getAttribute("currentExpert");
        } else {
            return this.expertFilter;
        }
    }

    void setExpertFilter(Expert expertFilter) {
        this.expertFilter = expertFilter;
        if (UI.getCurrent() != null) {
            UI.getCurrent().getSession()
                    .setAttribute("currentExpert", expertFilter);
        }
    }


    private final ValueChangeListener vclProject = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
            Project project = (Project) event.getProperty().getValue();
            if (project != null) {
                setProjectFilter(project);
                //menu.setProject(project);
                toolBar.setProject(project);
            } else {
                setProjectFilter(null);
                //menu.setProject(null);
                toolBar.setProject(project);
            }
            setExpertFilter(null);
            updateExpertsFilters();
            expertTable.resetFilters();
            expertTable.setValue(expertTable.firstItemId());
            expertTable.select(expertTable.firstItemId());
        }
    };

    private final ValueChangeListener vclExpert = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
            Expert expert = (Expert) event.getProperty().getValue();
            if (expert != null) setExpertFilter(expert);
            else setExpertFilter(null);
            clearRebukeFilters();
            updateRebukeData();
            rebukeTable.setValue(rebukeTable.firstItemId());
            rebukeTable.select(rebukeTable.firstItemId());
            updateAnswerFilters();
        }
    };
    private final ValueChangeListener vclRebuke = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
            Set<Rebuke> setRebuke;
            setRebuke = (Set<Rebuke>) event.getProperty().getValue();
            if (setRebuke != null) {
                rebukeFilter.clear();
                for (Rebuke rebuke : setRebuke) {
                    rebukeFilter.add(rebuke);
                }
            } else rebukeFilter.clear();
            updateAnswerFilters();
            answerTable.resetFilters();
            answerTable.setValue(answerTable.firstItemId());
        }
    };

    private final ValueChangeListener vclAnswer = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
        }
    };

    private final CellStyleGenerator answerCellStyleGenerator = new CellStyleGenerator() {
        @Override
        public String getStyle(CustomTable source, Object itemId, Object propertyId) {
            Answer a = (Answer) itemId;
            if (a == null || !a.getTotal())
                return null;
            else
                return "rebukestatus5";
        }
    };

    private final HorizontalSplitPanel panelCentral = new HorizontalSplitPanel();

    // Главная страница приложения
    @PostConstruct
    public void init() {
        setStyleName("mainwindow");
        projects = new BeanItemContainer<>(Project.class);
        experts = new BeanItemContainer<>(Expert.class);
        rebukes = new BeanItemContainer<>(Rebuke.class);
        answers = new BeanItemContainer<>(Answer.class);
        collapseButton.setIcon(ICON_LR);
        collapseButton.setWidth("50px");
        setSizeFull();
        //addComponent(menu); // Основное меню приложения
        addComponent(toolBar); // панель инструментов
        addComponent(centralPanel()); // Центральная часть приложения
        updateProjectsFilter();
        updateExpertsFilters();
        updateRebukeData();
        updateAnswerFilters();
        projectTable.setValue(projectTable.firstItemId());
        expertTable.setValue(expertTable.firstItemId());
        rebukeTable.setValue(rebukeTable.firstItemId());
        answerTable.setValue(answerTable.firstItemId());
    }

    // Центральная часть рабочего стола (для таблиц)
    private Component centralPanel() {
        panelCentral.setFirstComponent(leftArea());
        panelCentral.setSecondComponent(rightArea());
        panelCentral.setSplitPosition(40);
        panelCentral.setSizeFull();
        panelCentral.setStyleName("small previews");
        panelCentral.addStyleName("mystyle");
        addAttachListener(new AttachListener() {
            @Override
            public void attach(AttachEvent attachEvent) {

            }
        });
        return panelCentral;
    }

    // Левая область центральной панели (для Проектов и Э.З.)
    private Component leftArea() {
        VerticalSplitPanel panel = new VerticalSplitPanel();
        panel.setFirstComponent(projectArea());
        panel.setSecondComponent(expertArea());
        panel.setSplitPosition(75);
        panel.setStyleName("small previews");
        return panel;
    }

    // ***************************************************************
    // Верхняя часть левой области центральной панели (для Проектов)
    // ***************************************************************
    private Component projectArea() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        Label label = new Label("Объекты экспертизы");
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setMargin(true);
        horizontalLayout.setHeight("40");
        horizontalLayout.addComponent(label);
        horizontalLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
        updateProjectsFilter();
        projectEditor.addListener(new EditorSavedListener() {
            @Override
            public void editorSaved(EditorSavedEvent event) {
                BeanItem<Project> projectBean = (BeanItem<Project>) event.getSavedItem();
                projects.addBean(projectBean.getBean());
                projectRepo.saveAndFlush(projectBean.getBean());
                setProjectStatus();
                updateProjectsFilter();
                auditing.saveEvent(user, "Добавление/Редактирование ОБЪЕКТА " + projectBean.getBean().getCode());
            }
        });

        projectContextMenu = new ContextMenu();
        projectContextMenu.addItemClickListener(new ContextMenu.ContextMenuItemClickListener() {
            @Override
            public void contextMenuItemClicked(ContextMenu.ContextMenuItemClickEvent contextMenuItemClickEvent) {
                ContextMenu.ContextMenuItem clickedItem = (ContextMenu.ContextMenuItem) contextMenuItemClickEvent.getSource();
                if (clickedItem == projectMenuItemCreate) {
                    fnProjectCreate(); // Create object
                } else if (clickedItem == projectMenuItemEdit) {
                    fnProjectEdit();   // Edit object
                } else if (clickedItem == projectMenuItemDelete) {
                    fnProjectDelete(); // Delete object
                } else if (clickedItem == projectMenuItemRefresh) {
                    updateProjectsFilter();
                } else if (clickedItem == projectMenuItemClearFilter) {
                    projectTable.resetFilters();
                } else if (clickedItem == projectMenuItemReport) {
                    fnProjectReport();
                }
            }
        });

        projectContextMenu.setAsContextMenuOf(projectTable);
        projectContextMenu.setOpenAutomatically(false);
        projectContextMenu.setHideAutomatically(true);
        projectMenuItemCreate = projectContextMenu.addItem("Добавить новый объект");
        projectMenuItemCreate.setIcon(new ThemeResource("icons_mini/page_add.png"));
        projectMenuItemEdit = projectContextMenu.addItem("Редактировать объект");
        projectMenuItemEdit.setIcon(new ThemeResource("icons_mini/page_edit.png"));
        projectMenuItemDelete = projectContextMenu.addItem("Удалить объект");
        projectMenuItemDelete.setIcon(new ThemeResource("icons_mini/page_delete.png"));
        projectMenuItemDelete.setSeparatorVisible(true);
        projectMenuItemReport = projectContextMenu.addItem("Отчеты по объекту");
        projectMenuItemReport.setIcon(new ThemeResource("icons_mini/report.png"));
        projectMenuItemReport.setSeparatorVisible(true);
        projectMenuItemRefresh = projectContextMenu.addItem("Обновить таблицу");
        projectMenuItemRefresh.setIcon(new ThemeResource("icons_mini/arrow_refresh.png"));
        projectMenuItemClearFilter = projectContextMenu.addItem("Очистить фильтр");
        projectMenuItemClearFilter.setIcon(new ThemeResource("icons_mini/cross.png"));

        projectTable.addItemClickListener(new ItemClickListener() {
                                              @SuppressWarnings("unchecked")
                                              @Override
                                              public void itemClick(ItemClickEvent event) {
                                                  BeanItem<Project> projectBean = (BeanItem<Project>) projectTable.getItem(projectTable.getValue());
                                                  if (projectBean == null) return;
                                                  Project project = projectBean.getBean();
                                                  if (event.isDoubleClick()) {
                                                      if (!role.equals("GIP") && !role.equals("ADMIN")) {
                                                          Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
                                                          return;
                                                      } else if (user.equals(project.getBoss()) || role.equals("ADMIN")) {
                                                          final BeanItem<Project> item = (BeanItem<Project>) projectTable.getItem(projectTable.getValue());
                                                          if (item != null) {
                                                              projectEditor.projectEdit(item);
                                                              UI.getCurrent().addWindow(projectEditor);
                                                          }
                                                      }
                                                  } else {
                                                      fireComponentEvent();
                                                  }
                                              }
                                          }

        );
        if (projectTable.getColumnGenerator("date") == null)
            projectTable.addGeneratedColumn("date", columnGenerator);
        if (projectTable.getColumnGenerator("name") == null) {
            projectTable.addGeneratedColumn("name", columnGenerator);
        }
        if (projectTable.getColumnGenerator("code") == null) {
            projectTable.addGeneratedColumn("code", columnGenerator);
        }
        if (projectTable.getColumnGenerator("boss") == null) {
            projectTable.addGeneratedColumn("boss", columnGenerator);
        }
        projectTable.addValueChangeListener(vclProject);
        projectTable.setValue(projectTable.firstItemId());
        projectStats.setWidth("100%");

        setProjectStatus();

        layout.addComponent(horizontalLayout);
        layout.addComponent(projectTable);
        layout.addComponent(projectStats);
        layout.setExpandRatio(projectTable, 1);
        layout.setMargin(true);
        layout.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
            @Override
            public void layoutClick(LayoutEvents.LayoutClickEvent event) {
                if (event.getButton() == MouseEventDetails.MouseButton.RIGHT) {
                    if (role.equals("GIP") || role.equals("ADMIN")) {
                        projectContextMenu.open(event.getClientX(), event.getClientY());
                    }
                }
            }
        });
        return layout;
    }

    private void fnProjectReport() {
        //mainReport.init((Project) projectTable.getValue());
        mainReport.init(projectFilter);
        UI.getCurrent().addWindow(mainReport);
    }

    private void fnProjectCreate() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            Project project = new Project();
            project.setDate(new Date());
            project.setBoss(user);
            final BeanItem<Project> newProjectItem = new BeanItem<>(project);
            projectEditor.projectEdit(newProjectItem);
            UI.getCurrent().addWindow(projectEditor);
        }
    }

    private BeanItem<Project> item;

    private void fnProjectEdit() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            item = (BeanItem<Project>) projectTable.getItem(projectTable.getValue());
            if (item != null) {
                projectEditor.projectEdit(item);
                UI.getCurrent().addWindow(projectEditor);
            } else {
                Notification.show("Для редактирования ОБЪЕКТА, выберите его в таблице!", Type.ERROR_MESSAGE);
            }
        }
    }

    private void fnProjectDelete() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            Item item = projectTable.getItem(projectTable.getValue());
            if (item != null) {
                MsgDelete.msgShow(new MessageBoxListener() {
                    @Override
                    public void buttonClicked(
                            ButtonId buttonId) {
                        if (buttonId.toString().equals("mb_YES")) {
                            if (projects.removeItem(projectTable.getValue())) {
                                projectRepo.delete((Project) projectTable.getValue());
                                auditing.saveEvent(user, "Удаление ОБЪЕКТА " + ((Project) projectTable.getValue()).getCode());
                                projectTable.setValue(projectTable.firstItemId());
                                setProjectStatus();
                            }
                        }
                    }
                });
            } else {
                Notification.show("Для удаления ОБЪЕКТА, выберите его в таблице!", Type.ERROR_MESSAGE);
            }
        }
    }


    // **************************************************************************
    // Нижняя часть левой области центральной панели (для Э.З.)
    // ***************************************************************************
    private Component expertArea() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSpacing(true);
        horizontalLayout.setMargin(true);
        horizontalLayout.setWidth("100%");
        horizontalLayout.addComponent(new Label("Экспертные заключения"));
        updateExpertsFilters();
        expertEditor.addListener(new ExpertEditorSavedListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void expertEditorSaved(ExpertEditorSavedEvent event) {
                BeanItem<Expert> a = (BeanItem<Expert>) event.getSavedItem();
                expertRepo.saveAndFlush(a.getBean());
                auditing.saveEvent(user, "Добавление/Редактирование ЭЗ №" + a.getBean().getNumber() + " для ОБЪЕКТА " + a.getBean().getProject().getCode());
                experts.addBean(a.getBean());
                updateExpertsFilters();
                setExpertStatus();
            }
        });
        expertEditor.addCloseListener(new CloseListener() {
            @Override
            public void windowClose(CloseEvent closeEvent) {
                updateExpertsFilters();
                setExpertStatus();
            }
        });

        // Контекстное меню
        expertContextMenu = new ContextMenu();
        expertContextMenu.addItemClickListener(new ContextMenu.ContextMenuItemClickListener() {
            @Override
            public void contextMenuItemClicked(ContextMenu.ContextMenuItemClickEvent contextMenuItemClickEvent) {
                ContextMenu.ContextMenuItem clickedItem = (ContextMenu.ContextMenuItem) contextMenuItemClickEvent.getSource();
                if (clickedItem == expertMenuItemCreate) {
                    fnExpertCreate(); // Create object
                } else if (clickedItem == expertMenuItemEdit) {
                    fnExpertEdit();   // Edit object
                } else if (clickedItem == expertMenuItemDelete) {
                    fnExpertDelete(); // Delete object
                } else if (clickedItem == expertMenuItemTaskGip) {
                    fnExpertTaskGip();
                } else if (clickedItem == expertMenuItemSendAll) {
                    fnExpertSenAll();
                } else if (clickedItem == expertMenuItemCopy) {
                    fnExpertCopy();
                } else if (clickedItem == expertMenuItemExportXML) {
                    fnExpertExportXML();
                } else if (clickedItem == expertMenuItemImportXML) {
                    fnExpertImportXML();
                }
            }
        });

        expertContextMenu.setAsContextMenuOf(expertTable);
        expertContextMenu.setOpenAutomatically(false);
        expertContextMenu.setHideAutomatically(true);
        expertMenuItemCreate = expertContextMenu.addItem("Добавить экспертное заключение");
        expertMenuItemCreate.setIcon(new ThemeResource("icons_mini/page_add.png"));
        expertMenuItemEdit = expertContextMenu.addItem("Редактировать");
        expertMenuItemEdit.setIcon(new ThemeResource("icons_mini/page_edit.png"));
        expertMenuItemDelete = expertContextMenu.addItem("Удалить");
        expertMenuItemDelete.setIcon(new ThemeResource("icons_mini/page_delete.png"));
        expertMenuItemDelete.setSeparatorVisible(true);
        expertMenuItemTaskGip = expertContextMenu.addItem("Задание ГИП");
        expertMenuItemTaskGip.setIcon(new ThemeResource("icons_mini/bell.png"));
        expertMenuItemTaskGip.setSeparatorVisible(true);
        expertMenuItemSendAll = expertContextMenu.addItem("Отправить всем уведомления о назначении замечаний");
        expertMenuItemSendAll.setIcon(new ThemeResource("icons_mini/email_go.png"));
        expertMenuItemSendAll.setSeparatorVisible(true);
        expertMenuItemCopy = expertContextMenu.addItem("Копировать");
        expertMenuItemCopy.setIcon(new ThemeResource("icons_mini/page_copy.png"));
        expertMenuItemExportXML = expertContextMenu.addItem("Экспорт в XML");
        expertMenuItemExportXML.setIcon(new ThemeResource("icons_mini/page_white_put.png"));
        expertMenuItemImportXML = expertContextMenu.addItem("Импорт из XML");
        expertMenuItemImportXML.setIcon(new ThemeResource("icons_mini/page_white_get.png"));

        expertTable.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    if (!role.equals("GIP") && !role.equals("ADMIN")) {
                        Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
                        return;
                    }
                    final BeanItem<Expert> item = (BeanItem<Expert>) expertTable.getItem(expertTable.getValue());
                    if (item != null) {
                        expertEditor.expertEdit(item, true);
                        UI.getCurrent().addWindow(expertEditor);
                    } else {
                        Notification.show("Для редактирования ЭКСПЕРТНОГО ЗАКЛЮЧЕНИЯ, выберите его в таблице!", Type.ERROR_MESSAGE);
                    }
                }
            }
        });
        expertTable.addValueChangeListener(vclExpert);
        if (expertTable.getColumnGenerator("datecreate") == null) {
            expertTable.addGeneratedColumn("datecreate", columnGenerator);
        }
        if (expertTable.getColumnGenerator("statustext") == null) {
            expertTable.addGeneratedColumn("statustext", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    return generateIcon(source, itemId, columnId);
                }
            });
        }
        expertTable.setValue(expertTable.firstItemId());
        setExpertStatus();
        layout.addComponent(horizontalLayout);
        layout.addComponent(expertTable);
        layout.addComponent(expertStats);
        layout.addComponent(new Label("-"));
        layout.setExpandRatio(expertTable, 1);
        layout.setMargin(true);
        layout.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
            @Override
            public void layoutClick(LayoutEvents.LayoutClickEvent event) {
                if (event.getButton() == MouseEventDetails.MouseButton.RIGHT)
                    if (role.equals("GIP") || role.equals("ADMIN")) {
                        if (role.equals("GIP")) {
                            expertMenuItemImportXML.setEnabled(false);
                            expertMenuItemExportXML.setEnabled(false);
                        } else {
                            expertMenuItemImportXML.setEnabled(true);
                            expertMenuItemExportXML.setEnabled(true);
                        }
                        expertContextMenu.open(event.getClientX(), event.getClientY());
                    }
            }
        });
        return layout;
    }

    private void fnExpertSenAll() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            BeanItem<Expert> item = (BeanItem<Expert>) expertTable.getItem(expertTable.getValue());
            if (item != null) {
                // Рассылка уведомлений по отделам
                Set<Rebuke> rebukeList = item.getBean().getRebukes();
                sendMailAll.init(rebukeList);
                //Notification.show("Модуль в процессе разработки...", Type.WARNING_MESSAGE);
            } else {
                Notification.show("Для отправки уведомлений по ЭКСПЕРТНОМУ ЗАКЛЮЧЕНИЮ, выберите его в таблице!", Type.ERROR_MESSAGE);
            }
        }
    }

    protected final ColumnGenerator columnGenerator = new ColumnGenerator() {
        @Override
        public Object generateCell(CustomTable source, Object itemId, Object columnId) {
            Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
            Label label = new Label();
            if (prop.getType().equals(Date.class)) {
                if (source.getItem(itemId).getItemProperty(columnId).getValue() == null) {
                    label.setValue("");
                } else {
                    java.util.Calendar cal = new GregorianCalendar();
                    cal.setTime((Date) source.getItem(itemId).getItemProperty(columnId).getValue());
                    label.setValue(sdf.format(cal.getTime()));
                }
            } else if (prop.getType().equals(String.class)) {
                String s = (String) prop.getValue();
                label.setValue(s);
                label.setContentMode(ContentMode.TEXT);
                label.setStyleName("rebukecontext");
            }
            return label;
        }
    };

    private void fnExpertCreate() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (projectTable.getValue() == null) {
            Notification.show("Не задан объект!", Type.ERROR_MESSAGE);
        } else {
            final Expert ex = new Expert();
            ex.setDatecreate(new Date());
            ex.setDaterecive(new Date());
            ex.setDatemodify(new Date());
            ex.setForm("Официальное");
            ex.setType("Внешнее");
            ex.setStatustext(STR_INWORKS);
            ex.setProject(getProjectFilter());
            final BeanItem<Expert> newExpertItem = new BeanItem<>(ex);
            expertEditor.expertEdit(newExpertItem, false);
            UI.getCurrent().addWindow(expertEditor);
        }
    }

    private void fnExpertEdit() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            BeanItem<Expert> item = (BeanItem<Expert>) expertTable.getItem(expertTable.getValue());
            if (item != null) {
                expertEditor.expertEdit(item, true);
                UI.getCurrent().addWindow(expertEditor);
            } else {
                Notification.show("Для редактирования ЭКСПЕРТНОГО ЗАКЛЮЧЕНИЯ, выберите его в таблице!", Type.ERROR_MESSAGE);
            }
        }
    }

    private void fnExpertDelete() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            Item item = expertTable.getItem(expertTable.getValue());
            if (item != null) {
                // проверим наличие привязанных к ЭЗ файлов
                List<Filestorage> files = filestorageRepo.findByExpert((Expert) expertTable.getValue());
                if (files.isEmpty()) {
                    MsgDelete.msgShow(new MessageBoxListener() {
                        @Override
                        public void buttonClicked(ButtonId buttonId) {
                            if (buttonId.toString().equals("mb_YES")) {
                                if (experts.removeItem(expertTable.getValue())) {
                                    expertRepo.delete((Expert) expertTable.getValue());
                                    auditing.saveEvent(user, "Удаление ЭЗ №" + ((Expert) expertTable.getValue()).getNumber() + " для ОБЪЕКТА " + ((Expert) expertTable.getValue()).getProject().getCode());
                                    updateRebukeData();
                                    setExpertStatus();
                                }
                            }
                        }
                    });
                } else {
                    Notification.show("Для начала удалите привязанные файлы.", Type.ERROR_MESSAGE);
                }
            } else {
                Notification.show("Для удаления ЭКСПЕРТНОГО ЗАКЛЮЧЕНИЯ, выберите его в таблице!", Type.ERROR_MESSAGE);
            }
        }
    }

    private void fnExpertCopy() {
        if (!role.equals("GIP") && !role.equals("ADMIN")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            BeanItem<Expert> item = (BeanItem<Expert>) expertTable.getItem(expertTable.getValue());
            copyExpertWindow.init(item.getBean().getProject().getId(), item.getBean().getId());
            UI.getCurrent().addWindow(copyExpertWindow);
        }
    }

    private void fnExpertTaskGip() {
        taskGipWindow.initVar((Expert) expertTable.getValue());
        UI.getCurrent().addWindow((Window) taskGipWindow);
    }

    private void fnExpertExportXML() {
        BeanItem<Expert> item = (BeanItem<Expert>) expertTable.getItem(expertTable.getValue());
        exportXML.startExport(item.getBean());
    }

    private void fnExpertImportXML() {
        BeanItem<Project> item = (BeanItem<Project>) projectTable.getItem(projectTable.getValue());
        Project project = item.getBean();
        try {
            importXML.startImport(project);
            updateExpertsFilters();
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    // Конец работы с ЭЗ

    // Правая область центральной панели (для Замечаний и ответов)
    private Component rightArea() {
        VerticalSplitPanel panel = new VerticalSplitPanel();
        panel.setFirstComponent(rebukeArea());
        panel.setSecondComponent(answerArea());
        panel.setSplitPosition(65);
        panel.setStyleName("small previews");
        return panel;
    }

    // ******************************************************************************************
    // Верхняя часть правой области центральной панели (для Замечаний)
    // ******************************************************************************************
    private Component rebukeArea() {
        VerticalLayout layout = new VerticalLayout();
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeUndefined();
        layout.setSizeFull();
        horizontalLayout.setMargin(true);
        horizontalLayout.setSpacing(true);
        horizontalLayout.setWidth("100%");
        FormLayout depCombo = new FormLayout();
        depCombo.setSizeUndefined();
        depCombo.setMargin(false);
        FormLayout subCombo = new FormLayout();
        subCombo.setSizeUndefined();
        subCombo.setMargin(false);
        updateRebukeData();
        depCombo.addComponent(depComboBox);
        depComboBox.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                String s = (String) event.getProperty().getValue();
                if (s != null) {
                    subComboBox.setValue(null);
                    currentDepartmentRebuke = depRepo.findByTitle(s).getShorttitle();
                } else {
                    currentDepartmentRebuke = "";
                    rebukeFilter.clear();
                }
                updateRebukeData();
                updateAnswerFilters();
            }
        });
        subCombo.addComponent(subComboBox);
        subComboBox.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                String s = (String) event.getProperty().getValue();
                if (s != null) {
                    depComboBox.setValue(null);
                    currentDepartmentRebuke = subRepo.findByFullname(s).getShortname();
                } else {
                    currentDepartmentRebuke = "";
                    rebukeFilter.clear();
                }
                updateRebukeData();
                updateAnswerFilters();
            }
        });
        depComboBox.setCaption("к Отделам");
        depComboBox.setImmediate(true);
        depComboBox.setWidth("250px");
        depComboBox.setPageLength(0);
        depComboBox.setTextInputAllowed(false);
        List<Department> d = depRepo.findAll();
        for (Department depo : d) {
            depComboBox.addItem(depo.getTitle());
        }
        subComboBox.setCaption("к Субподрядчикам");
        subComboBox.setImmediate(true);
        subComboBox.setWidth("250px");
        subComboBox.setPageLength(0);
        subComboBox.setTextInputAllowed(false);
        List<Subcontractors> sub = subRepo.findAll();
        for (Subcontractors depo : sub) {
            subComboBox.addItem(depo.getFullname());
        }
        collapseButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                if (isRebukeExpand) {
                    isRebukeExpand = false;
                    panelCentral.setSplitPosition(40);
                    updateRebukeTable();
                    updateAnswerTable();
                } else {
                    isRebukeExpand = true;
                    panelCentral.setSplitPosition(0);
                    updateRebukeTable();
                    updateAnswerTable();
                }
                setRebukeStatus();
            }
        });
        Label rebukeLabel = new Label("Замечания");
        rebukeLabel.setWidth("85px");
        horizontalLayout.addComponent(rebukeLabel);
        horizontalLayout.addComponent(depCombo);
        horizontalLayout.addComponent(subCombo);
        horizontalLayout.addComponent(collapseButton);
        horizontalLayout.setComponentAlignment(rebukeLabel, Alignment.MIDDLE_LEFT);
        horizontalLayout.setComponentAlignment(depCombo, Alignment.TOP_RIGHT);
        horizontalLayout.setComponentAlignment(subCombo, Alignment.TOP_RIGHT);
        horizontalLayout.setComponentAlignment(collapseButton, Alignment.BOTTOM_RIGHT);
        horizontalLayout.setExpandRatio(depCombo, 1);
        if (!rebukeEditor.isAttached())
            rebukeEditor.addListener(new RebukeEditListener() {
                @Override
                public void rebukeEdit(RebukeEditor.RebukeEditEvent event) {
                    BeanItem<?> e = (BeanItem<?>) event.getRebukeItem();
                    Rebuke r = (Rebuke) e.getBean();
                    Set<Answer> a = r.getAnswers();
                    r.setAnswers(null);
                    if (r.getStatusanswer() == null) r.setStatusanswer(false);
                    rebukeRepo.saveAndFlush(r);
                    // Смотрим, есть-ли ответственный исполнитель
                    Answer ans = answerRepo.findByRebukeAndParentdep(r, true);
                    if (StringUtils.isNotEmpty(r.getMainperformer())) {
                        if (ans == null) {
                            // Если нет, то смотрим, есть-ли ответ для назначенного ответственным исполнителя
                            ans = answerRepo.findOneByRebukeAndRebuketotext(r, r.getMainperformer());
                            if (ans == null) {
                                // Если нет, то создаем новый
                                Answer answer = new Answer();
                                answer.setRebuketotext(r.getMainperformer());
                                answer.setRebuketo(true);
                                answer.setRebuke(r);
                                answer.setDatestatus(new Date());
                                answer.setAccepted(false);
                                answer.setStatus(false);
                                answer.setNumber(r.getNumber());
                                answer.setNumberpp(r.getNumberpp());
                                answer.setTotal(false);
                                answer.setAnswertext("");
                                answer.setParentdep(true);
                                answer.setClose(false);
                                answer.setFormulation(STR_NOTEACCEPTED);
                                answerRepo.saveAndFlush(answer);
                            } else {
                                ans.setParentdep(true);
                                answerRepo.saveAndFlush(ans);
                            }
                        } else {
                            // изменяем ответственного исполнителя
                            if (!ans.getRebuketotext().equals(r.getMainperformer())) {
                                // Снимаем текущего исполнителя с ответственных
                                ans.setParentdep(false);
                                answerRepo.saveAndFlush(ans);
                                // Смотрим, есть-ли ответ для исполителя
                                ans = answerRepo.findOneByRebukeAndRebuketotext(r, r.getMainperformer());
                                if (ans == null) {
                                    // Создаем новый ответ
                                    Answer answer = new Answer();
                                    answer.setRebuketotext(r.getMainperformer());
                                    answer.setRebuketo(true);
                                    answer.setRebuke(r);
                                    answer.setDatestatus(new Date());
                                    answer.setAccepted(false);
                                    answer.setStatus(true);
                                    answer.setNumber(r.getNumber());
                                    answer.setNumberpp(r.getNumberpp());
                                    answer.setTotal(false);
                                    answer.setAnswertext("");
                                    answer.setParentdep(true);
                                    answer.setClose(false);
                                    answer.setFormulation(STR_NOTEACCEPTED);
                                    answerRepo.saveAndFlush(answer);
                                } else {
                                    ans.setParentdep(true);
                                    answerRepo.saveAndFlush(ans);
                                }
                            }
                        }
                    }
                    updateAnswerFilters();
                    r.setAnswers(a);
                    updateRebukeData();
                    setRebukeStatus();
                }
            });
        selectHand.addListener(new SelectHandListener() {
            @Override
            public void selectHand(SelectHandEvent event) {
                updateAnswerFilters();
                updateRebukeTable();
                updateAnswerTable();
                rebukeTable.refreshRowCache();
                answerTable.refreshRowCache();
                setRebukeStatus();
                setAnswerStatus();
            }
        });
        setRaiseRebuke.addListener(new SetRaiseRebukeListener() {
            @Override
            public void setRaiseRebuke(SetRaiseRebukeEvent event) {
                updateAnswerFilters();
                rebukeTable.refreshRowCache();
                setRebukeStatus();
            }
        });
        // Контекстное меню
        rebukeContextMenu = new ContextMenu();
        rebukeContextMenu.addItemClickListener(new ContextMenu.ContextMenuItemClickListener() {
            @Override
            public void contextMenuItemClicked(ContextMenu.ContextMenuItemClickEvent contextMenuItemClickEvent) {
                ContextMenu.ContextMenuItem clickedItem = (ContextMenu.ContextMenuItem) contextMenuItemClickEvent.getSource();
                if (clickedItem == rebukeMenuItemCreate) {
                    fnRebukeCreate(); // Create object
                } else if (clickedItem == rebukeMenuItemCreateCopy) {
                    fnRebukeCreateCopy();   // Edit object
                } else if (clickedItem == rebukeMenuItemEdit) {
                    fnRebukeEdit();   // Edit object
                } else if (clickedItem == rebukeMenuItemDelete) {
                    fnRebukeDelete(); // Delete object
                } else if (clickedItem == rebukeMenuItemAppointExecutive) {
                    fnRebukeAppointExecutive();
                } else if (clickedItem == rebukeMenuItemDoneAnswer) {
                    fnRebukeDoneAnswer();
                } else if (clickedItem == rebukeMenuItemRaisRebuke) {
                    fnRebukeRaisRebuke();
                } else if (clickedItem == rebukeMenuItemSetRepeat) {
                    fnRebukeSetRepeat();
                } else if (clickedItem == rebukeMenuItemClearFilter) {
                    fnRebukeClearFilter();
                } else if (clickedItem == rebukeMenuItemPrint) {
                    fnRebukePrint();
                } else if (clickedItem == rebukeMenuItemselectAll) {
                    fnRebukeselectAll();
                }
            }
        });

        rebukeContextMenu.setAsContextMenuOf(rebukeTable);
        rebukeContextMenu.setOpenAutomatically(false);
        rebukeContextMenu.setHideAutomatically(true);
        rebukeMenuItemCreate = rebukeContextMenu.addItem("Добавить новое замечание");
        rebukeMenuItemCreate.setIcon(new ThemeResource("icons_mini/page_add.png"));
        rebukeMenuItemCreateCopy = rebukeContextMenu.addItem("Добавить копию выделенного");
        rebukeMenuItemCreateCopy.setIcon(new ThemeResource("icons_mini/page_copy.png"));
        rebukeMenuItemEdit = rebukeContextMenu.addItem("Редактировать");
        rebukeMenuItemEdit.setIcon(new ThemeResource("icons_mini/page_edit.png"));
        rebukeMenuItemDelete = rebukeContextMenu.addItem("Удалить");
        rebukeMenuItemDelete.setIcon(new ThemeResource("icons_mini/page_delete.png"));
        rebukeMenuItemDelete.setSeparatorVisible(true);
        rebukeMenuItemAppointExecutive = rebukeContextMenu.addItem("Назначить исполнителей");
        rebukeMenuItemAppointExecutive.setIcon(new ThemeResource("icons_mini/user_add.png"));
        rebukeMenuItemDoneAnswer = rebukeContextMenu.addItem("Изменить статус готовности");
        rebukeMenuItemDoneAnswer.setIcon(new ThemeResource("icons_mini/arrow_refresh.png"));
        rebukeMenuItemRaisRebuke = rebukeContextMenu.addItem("Снятие замечания");
        rebukeMenuItemRaisRebuke.setIcon(new ThemeResource("icons_mini/page_white_put.png"));
        rebukeMenuItemSetRepeat = rebukeContextMenu.addItem("Объявить повторным для...");
        rebukeMenuItemSetRepeat.setIcon(new ThemeResource("icons_mini/arrow_divide.png"));
        rebukeMenuItemClearFilter = rebukeContextMenu.addItem("Очистить фильтр");
        rebukeMenuItemClearFilter.setIcon(new ThemeResource("icons_mini/cross.png"));
        rebukeMenuItemClearFilter.setSeparatorVisible(true);
        rebukeMenuItemselectAll = rebukeContextMenu.addItem("Выделить все");
        rebukeMenuItemPrint = rebukeContextMenu.addItem("Распечатать");
        rebukeMenuItemPrint.setIcon(new ThemeResource("icons_mini/printer.png"));

        rebukeTable.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    if (role.equals("USER") &&
                            !getExpertFilter().getType().equals("Внутреннее")) {
                        Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
                        return;
                    }
                    final Set<Rebuke> setR = new HashSet<>();
                    setR.addAll((Set<Rebuke>) rebukeTable.getValue());
                    if (setR.isEmpty()) {
                        Notification.show("Для редактирования ЗАМЕЧАНИЯ, выберите его в таблице!", Type.ERROR_MESSAGE);
                        return;
                    }
                    if (setR.size() > 1) {
                        Notification.show("Выбрано более одного ЗАМЕЧАНИЯ!", Type.ERROR_MESSAGE);
                        return;
                    }
                    Rebuke r = setR.iterator().next();
                    if (role.equals("USER") && !r.getAutor().equals(user)) {
                        Notification.show("Вы не являетесь автором данного замечания", Type.ERROR_MESSAGE);
                        return;
                    }
                    final BeanItem<Rebuke> item = new BeanItem<>(r);
                    rebukeEditor.rebukeEdit(item, true, false);
                    UI.getCurrent().addWindow(rebukeEditor);
                }
            }
        });
        layout.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
            @Override
            public void layoutClick(LayoutEvents.LayoutClickEvent event) {
                if (event.getButton() == MouseEventDetails.MouseButton.RIGHT) {
                    if (!role.equals("USER")) {
                        rebukeContextMenu.open(event.getClientX(), event.getClientY());
                    }
                }
            }
        });

        rebukeTable.addValueChangeListener(vclRebuke);
        //rebukeTable.setCellStyleGenerator(rebukeCellStyleGenerator);
        if (rebukeTable.getColumnGenerator("status") == null)
            rebukeTable.addGeneratedColumn("status", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    return generateBoolean(source, itemId, columnId);
                }
            });
        if (rebukeTable.getColumnGenerator("statusanswer") == null)
            rebukeTable.addGeneratedColumn("statusanswer", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    return generateBoolean(source, itemId, columnId);
                }
            });
        if (rebukeTable.getColumnGenerator("content") == null) {
            rebukeTable.addGeneratedColumn("content", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
                    Label label = new Label();
                    // label.setStyleName("tablestatus");
                    if (prop.getType().equals(String.class)) {
                        String s = (String) prop.getValue();
                        label.setValue(s);
                        label.setContentMode(ContentMode.TEXT);
                        label.setStyleName("rebukecontext");
                    }
                    return label;
                }
            });
        }
        if (rebukeTable.getColumnGenerator("sectionproject") == null) {
            rebukeTable.addGeneratedColumn("sectionproject", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
                    Label label = new Label();
                    // label.setStyleName("tablestatus");
                    if (prop.getType().equals(String.class)) {
                        String s = (String) prop.getValue();
                        label.setValue(s);
                        label.setContentMode(ContentMode.TEXT);
                        label.setStyleName("rebukecontext");
                    }
                    return label;
                }
            });

        }
        if (rebukeTable.getColumnGenerator("codeerror") == null) {
            rebukeTable.addGeneratedColumn("codeerror", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
                    Label label = new Label();
                    if (prop.getType().equals(Integer.class)) {
                        if ((Integer) prop.getValue() != 0) {
                            label.setValue(String.valueOf(prop.getValue()));
                        } else {
                            label.setValue("");
                        }
                    }
                    return label;
                }
            });
        }

        rebukeStats.setWidth("100%");
        setRebukeStatus();
        layout.addComponent(horizontalLayout);
        layout.addComponent(rebukeTable);
        layout.addComponent(rebukeStats);
        layout.setExpandRatio(rebukeTable, 1);
        layout.setSizeFull();
        layout.setMargin(true);
        return layout;
    }

    private void fnRebukeselectAll() {
        rebukeTable.setValue(rebukeTable.getItemIds());
    }

    @Autowired
    private RebukePrint rebukePrint;

    private void fnRebukePrint() {
        if (((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
        } else {
            try {
                rebukePrint.startExport(projectFilter, (Collection<? extends Rebuke>) rebukeTable.getValue());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (JRException e) {
                e.printStackTrace();
            }
            //UI.getCurrent().addWindow(rebukePrint);
        }
    }


    private void fnRebukeCreate() {
        if (role.equals("USER") && !getExpertFilter().getType().equals("Внутреннее")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
            return;
        } else {
            if (getExpertFilter() != null) {
                Rebuke r = new Rebuke();
                r.setExpert(getExpertFilter());
                r.setRepeat(false);
                r.setStatus(false);
                final BeanItem<Rebuke> newRebukeItem = new BeanItem<>(r);
                rebukeEditor.rebukeEdit(newRebukeItem, false, false);
                UI.getCurrent().addWindow(rebukeEditor);
            } else {
                Notification.show("Для добавления замечания задайте ЭКСПЕРТНОЕ ЗАКЛЮЧЕНИЕ!", Type.ERROR_MESSAGE);
            }
        }
    }

    private void fnRebukeCreateCopy() {
        if (role.equals("USER") &&
                !getExpertFilter().getType().equals("Внутреннее")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (((Set<Rebuke>) rebukeTable.getValue()).size() > 1) {
            Notification.show("Можно выбрать только одно замечание!", Type.ERROR_MESSAGE);
        } else {
            Set<Rebuke> setr = (Set<Rebuke>) rebukeTable.getValue();
            Rebuke r = new Rebuke();
            for (Rebuke rr : setr) {
                r = rr;
            }
            r.setId(null);
            List<Rebuke> rebukeList = rebukeRepo.findLastRebuke(expertFilter.getId());
            Rebuke rebukeLast = new Rebuke();
            if (rebukeList != null && !rebukeList.isEmpty()) {
                rebukeLast = rebukeList.get(0);
            } else {
                rebukeLast = null;
            }
            if (rebukeLast != null) {
                r.setNumber(rebukeLast.getNumber() + 1);
                r.setNumberpp(rebukeLast.getNumberpp() + 1);
            } else {
                r.setNumber(1);
                r.setNumberpp(1);
            }
            r.setStatus(false);
            r.setStatusanswer(false);
            rebukeEditor.rebukeEdit(new BeanItem<>(r), false, true);
            UI.getCurrent().addWindow(rebukeEditor);
        }
    }

    private void fnRebukeEdit() {
        if (role.equals("USER") && !getExpertFilter().getType().equals("Внутреннее")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else {
            Set<Rebuke> setR = new HashSet<>();
            setR.addAll((Set<Rebuke>) rebukeTable.getValue());
            if (setR.isEmpty()) {
                Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
                return;
            }
            if (setR.size() > 1) {
                Notification.show("Выбрано более одного ЗАМЕЧАНИЯ!", Type.ERROR_MESSAGE);
                return;
            }
            Rebuke r = setR.iterator().next();
            if (role.equals("USER") && !r.getAutor().equals(user)) {
                Notification.show("Вы не являетесь автором данного замечания", Type.ERROR_MESSAGE);
                return;
            }
            BeanItem<Rebuke> item = new BeanItem<>(r);
            rebukeEditor.rebukeEdit(item, true, false);
            UI.getCurrent().addWindow(rebukeEditor);
        }
    }

    private void fnRebukeDelete() {
        if (role.equals("USER") || role.equals("OPP")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
        } else {
            MsgDelete.msgShow(new MessageBoxListener() {
                @Override
                public void buttonClicked(ButtonId buttonId) {
                    if (buttonId.toString().equals("mb_YES")) {
                        deleteRowsRebuke((Collection<? extends Rebuke>) rebukeTable.getValue());
                    }
                }
            });
        }
    }

    private void fnRebukeAppointExecutive() {
        if (role.equals("USER")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
        } else {
            rebukeSet.clear();
            rebukeSet.addAll((Collection<? extends Rebuke>) rebukeTable.getValue());
            selectHand.setRebukess(rebukeSet);
            UI.getCurrent().addWindow(selectHand);
        }
    }

    private void fnRebukeDoneAnswer() {
        if (role.equals("USER")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
            return;
        }
        if (((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
            return;
        }
        if (role.equals("OPP")) {
            if (((Set<Rebuke>) rebukeTable.getValue()).size() > 1) {
                Notification.show("Вы можете выбрать только одно замечание!", Type.ERROR_MESSAGE);
                return;
            }
            Set<Rebuke> set = (Set<Rebuke>) rebukeTable.getValue();
            Rebuke rebuke = new Rebuke();
            for (Rebuke r : set) {
                rebuke = r;
            }
            if (!rebuke.getMainperformer().equals(department)) {
                Notification.show("Вы не можете изменить статус этого замечания!", Type.ERROR_MESSAGE);
                return;
            }
        }
        if (!((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            for (Rebuke r : (Set<Rebuke>) rebukeTable.getValue()) {
                Boolean total = false;
                Boolean indicator = false;
                Set<Answer> a = r.getAnswers();
                if (r.getExpert().getType().equals("Внешнее")) {
                    for (Answer answer : a) {
                        if (!answer.getStatus()) {  // проверяем готовность ответов у данного замечания
                            indicator = true;
                            break;
                        }
                    }
                    for (Answer answer : a) {
                        if (answer.getTotal()) {  // проверяем наличие итогового ответа
                            total = true;
                            break;
                        }
                    }
                } else {
                    indicator = false;
                    total = true;
                }
                if (r.getStatusanswer()) {
                    r.setStatusanswer(false);
                    r.setStatus(false);
                } else {
                    if (indicator) {
                        Notification.show("У замечания №" + r.getNumber().toString()
                                + " не готовы ответы. Статус готовности не изменен", Type.ERROR_MESSAGE);
                        indicator = false;
                    } else if (total) {
                        r.setStatusanswer(true);
                        total = false;
                    } else {
                        Notification.show("У замечания №" + r.getNumber().toString()
                                + " не готов итоговый ответ. Статус готовности не изменен", Type.ERROR_MESSAGE);
                        total = false;
                        break;
                    }
                }
                r.setAnswers(null);
                rebukeRepo.saveAndFlush(r);
                r.setAnswers(a);
            }
            updateAnswerFilters();
            rebukeTable.refreshRowCache();
            setRebukeStatus();

        } else {
            Notification.show("Не выбрано ни одного замечания", Type.ERROR_MESSAGE);
        }
    }

    private void fnRebukeRaisRebuke() {
        if (role.equals("USER")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
            return;
        }
        if (((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
            return;
        }
        if (((Set<Rebuke>) rebukeTable.getValue()).size() > 1) {
            Notification.show("Вы можете выбрать только одно замечание!", Type.ERROR_MESSAGE);
            return;
        } else {
            Set<Rebuke> set = (Set<Rebuke>) rebukeTable.getValue();
            Rebuke rebuke = new Rebuke();
            for (Rebuke r : set) {
                rebuke = r;
            }
            if (!rebuke.getMainperformer().equals(department) && !role.equals("ADMIN")) {
                Notification.show("Вы не можете изменить статус снятия этого замечания! Оно назначено другому подразделению.", Type.ERROR_MESSAGE);
                return;
            }
            if (!rebuke.getStatusanswer()) {
                Notification.show("Вы не можете изменить статус снятия этого замечания, т.к. ответы не готовы!", Type.ERROR_MESSAGE);
                return;
            }
        }
        Integer numAnswer = 0;
        Set<Rebuke> rrs = (Set<Rebuke>) rebukeTable.getValue();
        for (Rebuke rr : rrs) {
            if (rr.getStatusanswer()) {
                numAnswer = numAnswer + 1;
            }
        }
        if (numAnswer == 0) {
            Notification.show("Вы не можете изменить статус снятия этих замечания, т.к. ответы не готовы!", Type.ERROR_MESSAGE);
            return;
        }
        setRaiseRebuke.setRebukes((Set<Rebuke>) rebukeTable.getValue());
        UI.getCurrent().addWindow(setRaiseRebuke);
    }

    private void fnRebukeSetRepeat() {
        if (role.equals("USER")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (!((Set<Rebuke>) rebukeTable.getValue()).isEmpty()) {
            repeatWindow.removeCloseListener(closeListener);
            repeatWindow.addCloseListener(closeListener);
            repeatWindow.setRebukeSet((Set<Rebuke>) rebukeTable.getValue());
            UI.getCurrent().addWindow(repeatWindow);
        } else {
            Notification.show("Не задано ни одного замечания", Type.ERROR_MESSAGE);
        }
    }

    private void fnRebukeClearFilter() {
        clearRebukeFilters();
    }

    private void deleteRowsRebuke(Collection<? extends Rebuke> value) {
        rebukeSet.clear();
        rebukeSet.addAll(value);
        rebukeRepo.delete(rebukeSet);
        // Ренумерация после удаления
        List<Rebuke> listRepeat = rebukeRepo.findByExpertAndRepeat(expertFilter, true);
        List<Rebuke> list = rebukeRepo.findByExpertOrderByNumberppAsc(expertFilter);
        List<Rebuke> rebukeCollection = new ArrayList<>();
        // проверякм пустые ссылки повторных замечаний
        Boolean b;
        for (Rebuke r : listRepeat) {
            b = false;
            for (Rebuke l : list) {
                if (r.getRepeatnumber() == l.getNumber()) {
                    b = true;
                }
            }
            if (!b) {
                r.setRepeat(false);
                r.setRepeatnumber(null);
                rebukeRepo.saveAndFlush(r);
            }
        }
        // Перенумерация
        list = rebukeRepo.findByExpertOrderByNumberppAsc(expertFilter);
        listRepeat = rebukeRepo.findByExpertAndRepeat(expertFilter, true);
        List<Rebuke> tmp = new ArrayList<>();
        Integer i = 1;
        for (Rebuke r : list) {
            for (Rebuke rrr : listRepeat) {
                if (rrr.getRepeatnumber() == r.getNumber()) {
                    rrr.setRepeatnumber(i);
                    tmp.add(rrr);
                }
            }
            r.setNumber(i);
            if (r.getAnswers() != null && r.getAnswers().size() > 0) {
                for (Answer a : r.getAnswers()) {
                    a.setNumber(i);
                    answerRepo.saveAndFlush(a);
                }
            }
            rebukeCollection.add(r);
            i = i + 1;
        }
        rebukeRepo.save(rebukeCollection);
        rebukeRepo.save(tmp);
        //
        updateRebukeData();
        updateAnswerFilters();
        rebukeTable.setValue(null);
        setRebukeStatus();
    }

    private CloseListener closeListener = new CloseListener() {
        @Override
        public void windowClose(CloseEvent closeEvent) {
            updateRebukeData();
        }
    };

    // ****************************************************************************************
    // Нижняя часть правой области центральной панели (для Ответов или
    // деталировки замечаний)
    // ****************************************************************************************
    private Component answerArea() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSpacing(true);
        horizontalLayout.setMargin(true);
        horizontalLayout.setWidth("100%");
        horizontalLayout.addComponent(new Label("Ответы на замечания"));
        updateAnswerFilters();
        answerEditor.addListener(new AnswerEditor.AnswerEditorSavedListener() {
            @Override
            public void answerEditorSaved(AnswerEditor.AnswerEditorSavedEvent event) {
                answerTable.refreshRowCache();
                rebukeTable.refreshRowCache();
                updateRebukeData();
                updateAnswerFilters();
                setAnswerStatus();
            }
        });
        answerEditorTotal.addListener(new AnswerEditorTotal.AnswerEditorSavedListener() {
            @Override
            public void answerEditorSaved(AnswerEditorTotal.AnswerEditorSavedEvent event) {
                answerTable.refreshRowCache();
                rebukeTable.refreshRowCache();
                updateRebukeData();
                setAnswerStatus();
            }
        });

        answerContextMenu = new ContextMenu();
        answerContextMenu.addItemClickListener(new ContextMenu.ContextMenuItemClickListener() {
            @Override
            public void contextMenuItemClicked(ContextMenu.ContextMenuItemClickEvent contextMenuItemClickEvent) {
                ContextMenu.ContextMenuItem clickedItem = (ContextMenu.ContextMenuItem) contextMenuItemClickEvent.getSource();
                if (clickedItem == answerMenuItemEdit) {
                    fnAnswerEdit();   // Edit object
                } else if (clickedItem == answerMenuItemDelete) {
                    fnAnswerDelete(); // Delete object
                } else if (clickedItem == answerMenuItemToTotal) {
                    fnAnswerToTotal();
                } else if (clickedItem == answerMenuItemFromTotal) {
                    fnAnswerFromTotal();
                } else if (clickedItem == answerMenuItemDone) {
                    fnAnswerDone();
                } else if (clickedItem == answerMenuItemClose) {
                    fnAnswerClose();
                } else if (clickedItem == answerMenuItemOpen) {
                    fnAnswerOpen();
                }
            }
        });

        answerContextMenu.setAsContextMenuOf(answerTable);
        answerContextMenu.setOpenAutomatically(false);
        answerContextMenu.setHideAutomatically(true);
        answerMenuItemEdit = answerContextMenu.addItem("Редактировать ответ");
        answerMenuItemEdit.setIcon(new ThemeResource("icons_mini/page_edit.png"));
        answerMenuItemDelete = answerContextMenu.addItem("Удалить");
        answerMenuItemDelete.setIcon(new ThemeResource("icons_mini/page_delete.png"));
        answerMenuItemToTotal = answerContextMenu.addItem("Добавить в итоговый");
        answerMenuItemToTotal.setIcon(new ThemeResource("icons_mini/cart_add.png"));
        answerMenuItemFromTotal = answerContextMenu.addItem("Убрать из итогового");
        answerMenuItemFromTotal.setIcon(new ThemeResource("icons_mini/cart_delete.png"));
        answerMenuItemFromTotal.setSeparatorVisible(true);
        answerMenuItemDone = answerContextMenu.addItem("Изменить статус готовности");
        answerMenuItemDone.setIcon(new ThemeResource("icons_mini/arrow_refresh.png"));
//        answerMenuItemClose = answerContextMenu.addItem("Запретить редактирование");
//        answerMenuItemClose.setIcon(new ThemeResource("icons_mini/flag_red.png"));
//        answerMenuItemOpen = answerContextMenu.addItem("Разрешить редактирование");
//        answerMenuItemOpen.setIcon(new ThemeResource("icons_mini/flag_green.png"));


        answerTable.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    Set<Answer> setA = new HashSet<>();
                    setA.addAll((Set<Answer>) answerTable.getValue());
                    if (setA.isEmpty()) {
                        Notification.show("Для редактирования ответа, выберите его в таблице!", Type.ERROR_MESSAGE);
                        return;
                    }
                    if (setA.size() > 1) {
                        Notification.show("Выбрано более одного ответа!", Type.ERROR_MESSAGE);
                        return;
                    }
                    Answer ans = setA.iterator().next();
                    if (!role.equals("ADMIN") && !role.equals("GIP") && !role.equals("OPP")) {
                        if (ans.getClose() == null) {
                            ans.setClose(false);
                            answerRepo.saveAndFlush(ans);
                        }
                        if (ans.getTotal()) {
                            Notification.show("Итоговый  ответ закрыт для редактирования!", Type.ERROR_MESSAGE);
                            return;
                        }
                        if (!ans.getRebuketotext().equals(department) && !ans.getRebuketotext().equals("")) {
                            Notification.show("Это ответ не Вашего подразделения!", Type.ERROR_MESSAGE);
                            return;
                        } else if (ans.getTotal()) {
                            Notification.show("Итоговый ответ закрыт для редактирования Вами!", Type.ERROR_MESSAGE);
                            return;
                        } else if (ans.getClose() || ans.getStatus()) {
                            Notification.show("Ответ закрыт для редактирования!", Type.ERROR_MESSAGE);
                            return;
                        }
                    }
                    if (ans.getTotal()) {
                        answerEditorTotal.answerEdit(ans, user, role);
                        UI.getCurrent().addWindow(answerEditorTotal);
                    } else {
                        answerEditor.answerEdit(ans, user, role);
                        UI.getCurrent().addWindow(answerEditor);
                    }
                }
            }
        });
        layout.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
            @Override
            public void layoutClick(LayoutEvents.LayoutClickEvent event) {
                if (event.getButton() == MouseEventDetails.MouseButton.RIGHT) {
                    if (role.equals("USER")) {
//                        answerMenuItemClose.setEnabled(false);
//                        answerMenuItemOpen.setEnabled(false);
                        answerMenuItemDelete.setEnabled(false);
                        answerMenuItemDone.setEnabled(false);
                        answerMenuItemFromTotal.setEnabled(false);
                        answerMenuItemToTotal.setEnabled(false);
                    } else {
//                        answerMenuItemClose.setEnabled(true);
//                        answerMenuItemOpen.setEnabled(true);
                        answerMenuItemDelete.setEnabled(true);
                        answerMenuItemDone.setEnabled(true);
                        answerMenuItemFromTotal.setEnabled(true);
                        answerMenuItemToTotal.setEnabled(true);
                    }
                    answerContextMenu.open(event.getClientX(), event.getClientY());
                }
            }
        });
        answerTable.addValueChangeListener(vclAnswer);
        if (answerTable.getCellStyleGenerator() == null)
            answerTable.setCellStyleGenerator(answerCellStyleGenerator);
        if (answerTable.getColumnGenerator("close") == null)
            answerTable.addGeneratedColumn("close", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    //return generateClose(source, itemId, columnId);
                    return generateBoolean(source, itemId, columnId);
                }
            });
        if (answerTable.getColumnGenerator("status") == null)
            answerTable.addGeneratedColumn("status", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    return generateBoolean(source, itemId, columnId);
                }
            });
        if (answerTable.getColumnGenerator("formulation") == null)
            answerTable.addGeneratedColumn("formulation", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
                    Label label = new Label();
                    if (prop.getType().equals(String.class)) {
                        String s = (String) prop.getValue();
                        if (StringUtils.isEmpty(s) || s.equals(STR_NOT_NOTEACCEPTED)) {
                            label.setValue("x");
                            label.setStyleName("white");
                        } else if (s.equals(STR_NOTEACCEPTED)) {
                            label.setValue("√√");
                            label.setStyleName("white");
                        } else if (s.equals(STR_NOTEACCEPTED_PARTIAL)) {
                            label.setValue("√");
                            label.setStyleName("white");
                        }
                    }
                    return label;
                }
            });
        if (answerTable.getColumnGenerator("parentdep") == null)
            answerTable.addGeneratedColumn("parentdep", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    return generateBoolean(source, itemId, columnId);
                }
            });
        if (answerTable.getColumnGenerator("answertext") == null)
            answerTable.addGeneratedColumn("answertext", new ColumnGenerator() {
                @Override
                public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                    Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
                    Property<?> tota = source.getItem(itemId).getItemProperty("total");
                    Property<?> incl = source.getItem(itemId).getItemProperty("answers");
                    Label label = new Label();
                    if ((Boolean) tota.getValue()) {
                        if ((Collection<Answer>) incl.getValue() == null) {
                            label.setValue("ИТОГОВЫЙ ОТВЕТ");
                        } else {
                            Integer s = ((Collection<Answer>) incl.getValue()).size();
                            label.setValue("ИТОГОВЫЙ ОТВЕТ. Включено ответов : " + String.valueOf(s));
                        }
                    } else if (prop.getType().equals(String.class)) {
                        String s = (String) prop.getValue();
                        label.setValue(s);
                        label.setContentMode(ContentMode.TEXT);
                        label.setStyleName("rebukecontext");
                    }
                    return label;
                }
            });
        answerStats.setWidth("100%");
        setAnswerStatus();
        layout.addComponent(horizontalLayout);
        layout.addComponent(answerTable);
        layout.setExpandRatio(answerTable, 1);
        layout.addComponent(answerStats);
        layout.addComponent(new Label("-"));
        layout.setSizeFull();
        layout.setMargin(true);
        return layout;
    }

    // Отработка контекстного меню ОТВЕТОВ
    private void fnAnswerEdit() {
        Set<Answer> setA = new HashSet<>();
        setA.addAll((Set<Answer>) answerTable.getValue());
        if (setA.isEmpty()) {
            Notification.show("Для редактирования ответа, выберите его в таблице!", Type.ERROR_MESSAGE);
        } else if (setA.size() > 1) {
            Notification.show("Выбрано более одного ответа!", Type.ERROR_MESSAGE);
        } else {
            Answer ans = setA.iterator().next();
            if (!role.equals("ADMIN") && !role.equals("GIP") && !role.equals("OPP")) {
                if (ans.getClose() == null) {
                    ans.setClose(false);
                    answerRepo.saveAndFlush(ans);
                }
                if (ans.getTotal()) {
                    Notification.show("Итоговый ответ закрыт для редактирования!", Type.ERROR_MESSAGE);
                    return;
                }
                if (!ans.getRebuketotext().equals(department) && !ans.getRebuketotext().equals("")) {
                    Notification.show("Это ответ не Вашего подразделения!", Type.ERROR_MESSAGE);
                    return;
                } else if (ans.getTotal()) {
                    Notification.show("Итоговый ответ закрыт для редактирования!", Type.ERROR_MESSAGE);
                    return;
                } else if (ans.getClose() || ans.getStatus()) {
                    Notification.show("Ответ закрыт для редактирования!", Type.ERROR_MESSAGE);
                    return;
                }
            }
            if (ans.getTotal()) {
                answerEditorTotal.answerEdit(ans, user, role);
                UI.getCurrent().addWindow(answerEditorTotal);
            } else {
                answerEditor.answerEdit(ans, user, role);
                UI.getCurrent().addWindow(answerEditor);
            }
        }
    }

    private void fnAnswerDelete() {
        if (role.equals("USER")) {
            Notification.show(NOTIFY_NOTRIGHT, Type.ERROR_MESSAGE);
        } else if (((Set<Answer>) answerTable.getValue()).isEmpty()) {
            Notification.show("Не выбрано ни одного ответа!", Type.ERROR_MESSAGE);
        } else {
            MsgDelete.msgShow(new MessageBoxListener() {
                @Override
                public void buttonClicked(ButtonId buttonId) {
                    if (buttonId.toString().equals("mb_YES")) {
                        Collection<Rebuke> rebukeCollection = (Collection<Rebuke>) rebukeTable.getValue();
                        Collection<Answer> answers1 = (Collection<Answer>) answerTable.getValue();
                        for (Rebuke rebuke : rebukeCollection) {
                            for (Answer a : answers1) {
                                if (a.getTotal()) {
                                    if (!a.getAnswers().isEmpty()) {
                                        for (Answer aa : a.getAnswers()) {
                                            aa.setParent(null);
                                            aa.setClose(false);
                                            aa.setDatestatus(new Date());
                                        }
                                    }
                                    answerRepo.save(a.getAnswers());
                                    continue;
                                }
                                a.setParent(null);
                                if (a.getRebuketotext().equals(rebuke.getMainperformer())) {
                                    rebuke.setMainperformer("");
                                    rebukeRepo.save(rebuke);
                                    rebukeRepo.flush();
                                }
                            }
                        }
                        answerRepo.deleteInBatch(answers1);
                        answerRepo.flush();
                        for (Rebuke rebuke : rebukeCollection) {
                            Answer t = answerRepo.findByRebukeAndTotal(rebuke, true);
                            if (t != null) {
                                if (t.getAnswers().isEmpty()) {
                                    answerRepo.delete(t);
                                    answerRepo.flush();
                                }
                            }
                        }
                        updateAnswerFilters();
                        setAnswerStatus();
                        updateRebukeData();
                    }
                }
            });
        }
    }

    private void fnAnswerToTotal() {
        if (((Collection<Answer>) answerTable.getValue()).isEmpty()) {
            Notification.show("Не выбрано ни одного ответа!", Type.ERROR_MESSAGE);
        } else {
            Collection<Rebuke> rebukeCollection = (Collection<Rebuke>) rebukeTable.getValue();
            for (Rebuke rebuke : rebukeCollection) {
                Collection<Answer> answers = (Collection<Answer>) answerTable.getValue();
                Collection<Answer> tmpanswers = new ArrayList<>();
                for (Answer a : answers) {
                    if (a.getRebuke().getId().equals(rebuke.getId()) && !a.getTotal()) tmpanswers.add(a);
                }
                if (tmpanswers.isEmpty()) continue;
                Answer totalAns = answerRepo.findByRebukeAndTotal(rebuke, true);
                if (totalAns == null) {
                    BeanUtils.copyProperties(tmpanswers.iterator().next(), totalAns = new Answer());
                    totalAns.setId(null);
                    totalAns.setAnswertext(null);
                    totalAns.setTotal(true);
                    totalAns.setAccepted(false);
                    totalAns.setStatus(true);
                    totalAns.setParentdep(false);
                    totalAns.setClose(false);
                    totalAns.setRebuketotext(null);
                    totalAns.setRebuke(rebuke);
                }
                totalAns.setDatestatus(new Date());
                for (Answer a : tmpanswers) {
                    a.setClose(true);
                    a.setParent(totalAns);
                    a.setDatestatus(new Date());
                }
                totalAns.setAnswers(tmpanswers);
                tmpanswers.add(totalAns);
                answerRepo.save(tmpanswers);
                answerRepo.flush();
                Answer t = answerRepo.findByRebukeAndTotal(rebuke, true);
                if (t.getAnswers().isEmpty()) {
                    answerRepo.delete(t);
                    answerRepo.flush();
                } else {
                    t.setFormulation(calcFormulation(t));
                    answerRepo.saveAndFlush(t);
                }
            }
            updateAnswerFilters();
            setAnswerStatus();
            updateRebukeData();
            setRebukeStatus();
        }
    }

    private void fnAnswerFromTotal() {
        if (((Collection<Answer>) answerTable.getValue()).isEmpty()) {
            Notification.show("Не выбрано ни одного ответа!", Type.ERROR_MESSAGE);
        } else {
            Collection<Rebuke> rebukeCollection = (Collection<Rebuke>) rebukeTable.getValue();
            for (Rebuke rebuke : rebukeCollection) {
                Collection<Answer> answers = (Collection<Answer>) answerTable.getValue();
                Collection<Answer> tmpanswers = new ArrayList<>();
                for (Answer a : answers) {
                    if (a.getRebuke().getId().equals(rebuke.getId()) && !a.getTotal()) {
                        tmpanswers.add(a);
                    }
                }
                Answer totalAns = answerRepo.findByRebukeAndTotal(rebuke, true);
                if (tmpanswers.isEmpty() || totalAns == null) {
                    continue;
                }
                totalAns.setDatestatus(new Date());
                for (Answer a : tmpanswers) {
                    a.setClose(false);
                    a.setParent(null);
                    a.setDatestatus(new Date());
                }
                totalAns.getAnswers().removeAll(tmpanswers);
                if (totalAns.getAnswers() == null) {
                    answerRepo.delete(totalAns);
                } else {
                    tmpanswers.add(totalAns);
                }
                answerRepo.save(tmpanswers);
                answerRepo.flush();
                Answer t = answerRepo.findByRebukeAndTotal(rebuke, true);
                if (t.getAnswers().isEmpty()) {
                    answerRepo.delete(t);
                    answerRepo.flush();
                } else {
                    t.setFormulation(calcFormulation(t));
                    answerRepo.saveAndFlush(t);
                }
            }
            updateAnswerFilters();
            setAnswerStatus();
            updateRebukeData();
            setRebukeStatus();
        }
    }

    private void fnAnswerDone() {
        if (((Set<Answer>) answerTable.getValue()).isEmpty()) {
            Notification.show("Не выбрано ни одного ответа!", Type.ERROR_MESSAGE);
        } else {
            Set<Answer> setanswer = new HashSet<>();
            setanswer.addAll((Set<Answer>) answerTable.getValue());
            if (setanswer.isEmpty()) return;
            for (Answer answer : setanswer) {
                if (answer.getStatus()) {
                    answer.setStatus(false);
                } else {
                    answer.setStatus(true);
                }
                answerRepo.saveAndFlush(answer);
            }
            updateAnswerFilters();
            setAnswerStatus();
            rebukeTable.refreshRowCache();
            updateRebukeData();
            setRebukeStatus();
        }
    }

    private void fnAnswerClose() {
        Set<Answer> setcloseanswer = new HashSet<>();
        setcloseanswer.addAll((Set<Answer>) answerTable.getValue());
        for (Answer a : setcloseanswer) {
            a.setClose(true);
            answerRepo.saveAndFlush(a);
        }
        updateAnswerFilters();
        answerTable.setValue(answerTable.getValue());
        answerTable.refreshRowCache();
        setAnswerStatus();
    }

    private void fnAnswerOpen() {
        Set<Answer> setcloseanswer = new HashSet<>();
        setcloseanswer.addAll((Set<Answer>) answerTable.getValue());
        for (Answer a : setcloseanswer) {
            a.setClose(false);
            answerRepo.saveAndFlush(a);
        }
        updateAnswerFilters();
        answerTable.setValue(answerTable.getValue());
        answerTable.refreshRowCache();
        setAnswerStatus();
    }

    private String calcFormulation(Answer answer) {
        Boolean boolYes = false, boolNo = false, boolYesNo = false;
        for (Answer a : answer.getAnswers()) {
            if (StringUtils.isNotEmpty(a.getFormulation()))
                if (a.getFormulation().equals(STR_NOTEACCEPTED)) boolYes = true;
                else if (a.getFormulation().equals(STR_NOT_NOTEACCEPTED)) boolNo = true;
                else if (a.getFormulation().equals(STR_NOTEACCEPTED_PARTIAL)) boolYesNo = true;
        }
        if (boolYesNo) return STR_NOTEACCEPTED_PARTIAL;
        else if (boolYes && boolNo) return STR_NOTEACCEPTED_PARTIAL;
        else if (boolYes && !boolNo) return STR_NOTEACCEPTED;
        else if (!boolYes && boolNo) return STR_NOT_NOTEACCEPTED;
        else return STR_NOT_NOTEACCEPTED;
    }

    // *****************************************************************************************
    // Разные Update's
    private void updateProjectTable() {
        projectTable.setContainerDataSource(projects);
        projectTable.setFilterBarVisible(true);
        projectTable.setFilterDecorator(new ProjectFilterDecorator());
        projectTable.setSizeFull();
        projectTable.setSelectable(true);
        projectTable.setImmediate(true);
        projectTable.setColumnCollapsingAllowed(true);
        projectTable.setVisibleColumns("date", "code", "name", "boss");
        projectTable.setColumnHeaders("Дата", "Шифр", "Наименование", "ГИП");
        projectTable.setNullSelectionAllowed(false);
        projectTable.setColumnWidth("date", 70);
        projectTable.setColumnWidth("code", 125);
        //projectTable.setColumnWidth("boss", 100);
        projectTable.setColumnExpandRatio("name", 1);
        projectTable.setSortAscending(false);
        projectTable.setSortContainerPropertyId("date");
        projectTable.sort();
        projectTable.setCacheRate(50);
        //projectTable.setColumnCollapsed("date", true);
    }

    private void updateExpertTable() {
        //rebukeTable.setLocale(new Locale("ru", "RU"));
        //expertTable.setCaption("Экспертные заключения");
        expertTable.setContainerDataSource(experts);
        expertTable.setFilterBarVisible(false);
        if (expertTable.getFilterDecorator() == null)
            expertTable.setFilterDecorator(new ExpertFilterDecorator());
        expertTable.setSizeFull();
        expertTable.setSelectable(true);
        expertTable.setImmediate(true);
        expertTable.setColumnCollapsingAllowed(true);
        expertTable.setVisibleColumns("statustext", "datecreate", "number", "strongcompany");
        expertTable.setColumnHeaders("", "Дата", "Номер", "Экспертная организация");
        expertTable.setNullSelectionAllowed(false);
        expertTable.setColumnWidth("statustext", 12);
        expertTable.setColumnWidth("datecreate", 100);
        expertTable.setColumnExpandRatio("strongcompany", 1);
        expertTable.setFilterFieldVisible("statustext", false);
        expertTable.setSortAscending(false);
        expertTable.setSortContainerPropertyId("datecreate");
        expertTable.sort();
        if (expertTable.getItemDescriptionGenerator() == null) {
            expertTable.setItemDescriptionGenerator(new AbstractSelect.ItemDescriptionGenerator() {
                @Override
                public String generateDescription(Component component, Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return null;
                    } else if (propertyId == "statustext") {
                        return "Состояние ЭКСПЕРТНОГО ЗАКЛЮЧЕНИЯ";
                    }
                    return null;
                }
            });

        }
    }

    private void updateRebukeTable() {
        rebukeTable.setContainerDataSource(rebukes);
        if (rebukeTable.getFilterDecorator() == null)
            rebukeTable.setFilterDecorator(new RebukeFilterDecorator());
        if (rebukeTable.getColumnGenerator("accept") == null)
            rebukeTable.addGeneratedColumn("accept", new AcceptColumnGenerator());
        rebukeTable.setFilterBarVisible(true);
        rebukeTable.setBuffered(true);
        rebukeTable.setSizeFull();
        rebukeTable.setImmediate(true);
        rebukeTable.setSelectable(true);
        rebukeTable.setMultiSelect(true);
        rebukeTable.setMultiSelectMode(MultiSelectMode.DEFAULT);
        rebukeTable.setColumnCollapsingAllowed(true);
        if (!isRebukeExpand) {
            rebukeTable.setVisibleColumns("number", "accept", "statusanswer", "status", "repeatnumber",
                    "sectionproject", "content", "mainperformer");
            rebukeTable.setColumnHeaders("№", "П", "Г", "С", "Пов.", "Раздел проекта", "Содержание замечания", "Отв.");
            rebukeTable.setColumnCollapsed("sectionproject", true);
        } else {
            rebukeTable.setVisibleColumns("number", "numberpp", "accept", "statusanswer", "status", "repeatnumber",
                    "sectionproject", "content", "mainperformer", "codeerror");
            rebukeTable.setColumnHeaders("№пп", "Пов.", "П", "Г", "С", "№", "Раздел проекта",
                    "Содержание замечания", "Отв.", "Ошибка");
            rebukeTable.setColumnExpandRatio("content", 1);
            rebukeTable.setColumnCollapsed("sectionproject", false);
        }
        rebukeTable.setColumnWidth("accept", 20);
        rebukeTable.setColumnWidth("status", 20);
        rebukeTable.setColumnWidth("statusanswer", 20);
        rebukeTable.setColumnWidth("number", 25);
        rebukeTable.setColumnWidth("numberpp", 25);
        rebukeTable.setColumnWidth("repeatnumber", 25);
        rebukeTable.setColumnWidth("numberpage", 100);
        rebukeTable.setColumnWidth("sectionproject", 100);
        rebukeTable.setColumnWidth("codeerror", 50);
        rebukeTable.setColumnWidth("mainperformer", 60);
        rebukeTable.setColumnExpandRatio("content", 1);
        rebukeTable.setColumnAlignment("accept", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("status", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("statusanswer", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("number", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("numberpp", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("repeatnumber", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("mainperformer", CustomTable.Align.CENTER);
        rebukeTable.setColumnAlignment("codeerror", CustomTable.Align.CENTER);
        rebukeTable.setNullSelectionAllowed(false);
        rebukeTable.setFilterFieldVisible("mainperformer", false);
        rebukeTable.setFilterFieldVisible("accept", false);
        rebukeTable.setCacheRate(50);
        rebukeTable.setSortEnabled(true);
        rebukeTable.setSortContainerPropertyId("number");
        rebukeTable.setSortAscending(true);
        rebukeTable.sort();
        if (rebukeTable.getItemDescriptionGenerator() == null) {
            rebukeTable.setItemDescriptionGenerator(new AbstractSelect.ItemDescriptionGenerator() {
                @Override
                public String generateDescription(Component component, Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return null;
                    } else if (propertyId == "accept") {
                        return "Статус замечания - ПРИНЯТО / ЧАСТИЧНО ПРИНЯТО / НЕ ПРИНЯТО";
                    } else if (propertyId == "statusanswer") {
                        return "Статус замечания - ГОТОВО / НЕ ГОТОВО";
                    } else if (propertyId == "status") {
                        return "Статус замечания - СНЯТО / НЕ СНЯТО";
                    } else if (propertyId == "number") {
                        return "Номер замечания";
                    } else if (propertyId == "numberpp") {
                        return "Номер замечания порядковый";
                    } else if (propertyId == "repeatnumber") {
                        return "Номер замечания, для которого данное замечание является повторным";
                    } else if (propertyId == "mainperformer") {
                        return "Ответственное подразделение";
                    }
                    return null;
                }
            });

        }
    }

    private Integer getAccepted(Set<Answer> answers) {
        Integer st = 0;
        Boolean isTotal = false;
        for (Answer a : answers) {
            if (a.getTotal()) {
                isTotal = true;
                if (a.getFormulation() == null) st = 0;
                else if (a.getFormulation().equals(STR_NOTEACCEPTED)) st = 2;
                else if (a.getFormulation().equals(STR_NOTEACCEPTED_PARTIAL)) st = 1;
                break;
            }
        }
        if (!isTotal) {
            for (Answer a : answers) {
                if (a.getParentdep()) {
                    if (a.getFormulation() == null) st = 0;
                    else if (a.getFormulation().equals(STR_NOTEACCEPTED)) st = 2;
                    else if (a.getFormulation().equals(STR_NOTEACCEPTED_PARTIAL)) st = 1;
                    break;
                }
            }
        }
        return st;
    }

    class AcceptColumnGenerator implements FilterTable.ColumnGenerator {
        @Override
        public Object generateCell(CustomTable source, Object itemId, Object columnId) {
            Label label = new Label();
            Integer st = getAccepted((Set<Answer>) source.getContainerProperty(itemId, "answers").getValue());
            if (st == 1) {
                label.setValue("√");
                label.setStyleName("white");
            } else if (st == 2) {
                label.setValue("√√");
                label.setStyleName("white");
            } else {
                label.setValue("x");
                label.setStyleName("white");
            }
            return label;
        }
    }

    private void updateAnswerTable() {
        answerTable.setContainerDataSource(answers);
//        if (answerTable.getFilterDecorator() == null)
//            answerTable.setFilterDecorator(new AnswerFilterDecorator());
        answerTable.setFilterBarVisible(false);
        answerTable.setBuffered(true);
        answerTable.setSizeFull();
        answerTable.setSelectable(true);
        answerTable.setMultiSelect(true);
        answerTable.setMultiSelectMode(MultiSelectMode.DEFAULT);
        answerTable.setImmediate(true);
        if (!isRebukeExpand) {
            answerTable.setVisibleColumns("number", "formulation", "status", "answertext", "parentdep", "rebuketotext");
            answerTable.setColumnHeaders("№", "П", "Г", "Содержание ответа", "Отв..", "Исполнитель");
        } else {
            answerTable.setVisibleColumns("number", "numberpp", "formulation", "status", "autor", "answertext", "comment",
                    "parentdep", "rebuketotext", "datestatus");
            answerTable.setColumnHeaders("№", "№пп", "П", "Г", "Автор", "Содержание ответа", "Комментарий",
                    "Отв.", "Исполнитель", "Дата");
            answerTable.setColumnWidth("numberpp", 25);
            answerTable.setColumnWidth("comment", 300);
            answerTable.setColumnExpandRatio("answertext", 1);
        }
        answerTable.setColumnWidth("formulation", 20);
        answerTable.setColumnWidth("status", 20);
        answerTable.setColumnWidth("close", 20);
        answerTable.setColumnWidth("number", 25);
        answerTable.setColumnWidth("autor", 200);
        answerTable.setColumnWidth("parentdep", 20);
        answerTable.setColumnExpandRatio("answertext", 1);
        answerTable.setColumnAlignment("close", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("accepted", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("status", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("number", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("numberpp", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("parentdep", CustomTable.Align.CENTER);
        answerTable.setColumnAlignment("close", CustomTable.Align.CENTER);
        answerTable.setFilterFieldVisible("formulation", false);
        answerTable.setNullSelectionAllowed(false);
        answerTable.setColumnCollapsingAllowed(true);
        answerTable.setCacheRate(50);
        answerTable.setSortEnabled(true);
        answerTable.setSortContainerPropertyId("total");
        answerTable.setSortAscending(false);
        answerTable.sort();
        if (answerTable.getItemDescriptionGenerator() == null) {
            answerTable.setItemDescriptionGenerator(new AbstractSelect.ItemDescriptionGenerator() {
                @Override
                public String generateDescription(Component component, Object itemId, Object propertyId) {
                    if (propertyId == null) {
                        return null;
                    } else if (propertyId == "formulation") {
                        return "Замечание ПРИНЯТО / ЧАСТИЧНО ПРИНЯТО / НЕ ПРИНЯТО";
                    } else if (propertyId == "status") {
                        return "Статус ответа - ГОТОВ / НЕ ГОТОВ";
                    } else if (propertyId == "close") {
                        return "Статус ответа - ЗАКРЫТ ДЛЯ РЕДАКТИРОВАНИЯ / ОТКРЫТ";
                    } else if (propertyId == "number") {
                        return "Номер замечания";
                    } else if (propertyId == "numberpp") {
                        return "Номер замечания порядковый";
                    } else if (propertyId == "parentdep") {
                        return "Исполнитель является ответственным";
                    }
                    return null;
                }
            });

        }

    }

    private void setProjectStatus() {
        projectStats.setValue("Всего объектов: " + String.valueOf(projectRepo.findAll().size()));
    }

    private void updateProjectsFilter() {
        setProjectStatus();
        projects = new BeanItemContainer<>(Project.class, projectRepo.findAll());
        updateProjectTable();
    }

    private void setExpertStatus() {
        expertStats.setValue("Всего экспертных заключений: " + String.valueOf(expertRepo.findByProject(getProjectFilter()).size()));
    }

    private void updateExpertsFilters() {
        Expert expert = (Expert) expertTable.getValue();
        Integer id = 0;
        if (expert != null) id = expert.getId();
        setExpertStatus();
        if (getProjectFilter() != null)
            experts = new BeanItemContainer<>(Expert.class, expertRepo.findByProject(getProjectFilter()));
        else
            experts = new BeanItemContainer<>(Expert.class);
        updateExpertTable();
        Collection<Expert> col = (Collection<Expert>) expertTable.getItemIds();
        if (col != null)
            for (Expert e : col) {
                if (e.getId().equals(id)) {
                    expertTable.setValue(e);
                }
            }
    }

    private void setRebukeStatus() {
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        List<Rebuke> r = rebukeRepo.findByExpert(getExpertFilter());
        for (Rebuke rebuke : r) {
            Set<Answer> ans = rebuke.getAnswers();
            for (Answer answer : ans) {
                if (answer != null) {
                    if (answer.getRebuketo() && StringUtils.isNotEmpty(answer.getRebuketotext())) {
                        c++;
                    } else if (!answer.getRebuketo() && !answer.getRebuketotext().isEmpty()) {
                        d++;
                    }
                }
            }
            if (rebuke.getStatus()) {
                a++;
            }
            if (rebuke.getStatusanswer()) {
                b++;
            }
        }
        String stat = "Всего замечаний: " + String.valueOf(r.size());
        rebukeStats.setValue(stat + ";    Из них снято: " + String.valueOf(a) + "; Готово ответов для: " + String.valueOf(b)
                + "; Назначено для отделов: " + String.valueOf(c) + "; для субподрядчиков: " + String.valueOf(d));
    }

    private void updateRebukeData() {
        Collection<Rebuke> rebs = (Collection<Rebuke>) rebukeTable.getValue();
        List<Integer> id = new ArrayList<>();
        if (rebs != null) {
            for (Rebuke r : rebs)
                id.add(r.getId());
        }
        rebukes.removeAllItems();
        if (getExpertFilter() != null) {
            if (StringUtils.isBlank(currentDepartmentRebuke))
                rebukes.addAll(rebukeRepo.findByExpertOrderByNumberppAsc(getExpertFilter()));
            else
                rebukes.addAll(rebukeRepo.findByRebukeToAnswerToDepartmentToShortTitle(currentDepartmentRebuke, getExpertFilter().getId()));
        }
        setRebukeStatus();
        updateRebukeTable();
        Collection<Rebuke> reb = (Collection<Rebuke>) rebukeTable.getItemIds();
        if (reb != null) {
            Collection<Rebuke> after = new LinkedList<>();
            for (Rebuke r : reb) {
                for (Integer i : id) {
                    if (r.getId().equals(i)) {
                        after.add(r);
                        //rebukeTable.setCurrentPageFirstItemId(r);
                    }
                }
            }
            rebukeTable.setValue(after);
        }
    }

    private void setAnswerStatus() {
        answerStats.setValue("Всего ответов: " + String.valueOf(answers.getItemIds().size()));
    }

    private void updateAnswerFilters() {
        Collection<Answer> answ = (Collection<Answer>) answerTable.getValue();
        List<Integer> id = new ArrayList<>();
        if (answ != null) {
            for (Answer a : answ)
                id.add(a.getId());
        }
        answers.removeAllItems();
        for (Rebuke rebuke : rebukeFilter)
            answers.addAll(answerRepo.findByRebuke(rebuke));
        updateAnswerTable();
        Collection<Answer> tmp = (Collection<Answer>) answerTable.getItemIds();
        if (tmp != null) {
            Collection<Answer> after = new LinkedList<>();
            for (Answer a : tmp) {
                for (Integer i : id) {
                    if (a.getId().equals(i)) {
                        after.add(a);
                    }
                }
            }
            answerTable.setValue(after);
        }
        setAnswerStatus();
    }

    private void clearRebukeFilters() {
        rebukeFilter.clear();
        rebukeTable.resetFilters();
        updateRebukeTable();
    }

    // Вместо логического значения показываем крыжик
    private Label generateBoolean(CustomTable source, Object itemId, Object columnId) {
        Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
        if (prop.getType().equals(Boolean.class)) {
            Label label = new Label();
            if ((prop.getValue() == null) || (prop.getValue().equals(false))) {
                label.setValue("");
                return label;
            } else {
                label.setValue("√");
                label.setStyleName("white");
                return label;
            }
        }
        return null;
    }

    // Вместо строки состояния ЭЗ показываем значок
    private Image generateIcon(CustomTable source, Object itemId, Object columnId) {
        Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
        if (prop.getType().equals(String.class)) {
            if (prop.getValue() != null) {
                if (prop.getValue().equals(STR_INWORKS)) {
                    return new Image(null, ICON_FORWARD);
                } else if (prop.getValue().equals(STR_ENDWORKS)) {
                    return new Image(null, ICON_STOPWORK);
                } else if (prop.getValue().equals(STR_SENDWORKS)) {
                    return new Image(null, ICON_BACKWARD);
                }
            } else {
                return new Image(null, ICON_WHITE);
            }
        }
        return null;
    }

    @Override
    public void enter(final ViewChangeEvent event) {
        //menu.addUserName();
        user = UI.getCurrent().getSession().getAttribute("user").toString();
        role = UI.getCurrent().getSession().getAttribute("role").toString();
        department = UI.getCurrent().getSession().getAttribute("department").toString();
        UI.getCurrent().getSession().setAttribute("currentProject", getProjectFilter());
        UI.getCurrent().getSession().setAttribute("currentExpert", getExpertFilter());
        if (!role.equals("ADMIN") && !role.equals("GIP")) {
            //menu.service.setVisible(false);
            toolBar.ctxMenuReport.setEnabled(false);
            toolBar.ctxMenuCatalog.setEnabled(false);
            toolBar.ctxMenuAdmin.setEnabled(false);
        }
        if (!role.equals("ADMIN")) {
            //menu.admin.setVisible(false);
        }
        // Проверим уровень доступа юзера и установим просмотр замечаний только для его отдела
        if ("USER".equals(role)) {
            depComboBox.setValue(depRepo.findByShorttitle(department).getTitle());
        } else {
            depComboBox.setValue(null);
        }

        if (role.equals("GIP")) {
            checkOverdue(user);
        }

    }

    private void checkOverdue(String user) {
        //Set<Project> projects = new HashSet<>();
        Date today = new Date();
        Set<Project> projects = projectRepo.findProjectToUser(user);
        if (checkEnable.equals("true")) {
            for (Project p : projects) {
                List<Expert> expertSet = expertRepo.findByProject(p);
                for (Expert e : expertSet) {
                    if (e.getDateansver().before(today) && e.getStatustext().equals(STR_INWORKS)) {
                        Notification.show("Срок ответа на замечания ИСТЕК!\n" +
                                "Объект - " + p.getCode().trim() + "\n" +
                                "Экспертное заключение №" + e.getNumber() + " от " + sdf1.format(e.getDatecreate()) + "\n" +
                                "Срок ответа на него " + sdf1.format(e.getDateansver()), Type.ERROR_MESSAGE);
                    } else if (e.getDateansver().equals(today) && e.getStatustext().equals(STR_INWORKS)) {
                        Notification.show("Срок ответа на замечания ИСТЕКАЕТ СЕГОДНЯ!\n" +
                                "Объект - " + p.getCode().trim() + "\n" +
                                "Экспертное заключение №" + e.getNumber() + " от " + sdf1.format(e.getDatecreate()) + "\n" +
                                "Срок ответа на него " + sdf1.format(e.getDateansver()), Type.ERROR_MESSAGE);
                    }

                }
            }
        }
    }


}
