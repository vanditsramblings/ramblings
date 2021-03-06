package com.rambler.tasklipse.views;


import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.rambler.tasklipse.comparator.TasklipseTableComparator;
import com.rambler.tasklipse.filter.TaskFilter;
import com.rambler.tasklipse.model.Task;
import com.rambler.tasklipse.model.TaskColumn;
import com.rambler.tasklipse.provider.HyperLinkLabelProvider;
import com.rambler.tasklipse.provider.TasklipseLabelProvider;
import com.rambler.tasklipse.service.TaskService;
import com.rambler.tasklipse.service.impl.TaskServiceImpl;

/**
 * @author vandit
 * 
 */

public class TasklipseView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.rambler.tasklipse.views.TasklipseView";

	
	//View Components
	Table table										=null;
	Composite composite								=null;
	CheckboxTableViewer viewer						=null;
	
	//Comparator
	TasklipseTableComparator comparator				=null;
	
	//Provider
	TasklipseLabelProvider labelProvider			=null;

	//Resource change listener
	IResourceChangeListener resourceChangedListener =null;
	
	//Filter
	TaskFilter taskFilter=null;
	
	//Services
	TaskService taskService=null;

	public static boolean TASKLIPSE_PROP_RESOURCE_CHANGE_LISTENER=true;

	ArrayList<Task> tasks=new ArrayList<Task>();
	private final Image CHECKED = getImage("checked.png");
	private final Image UNCHECKED = getImage("unchecked.png");
	
	/**
	 * The constructor.
	 */
	public TasklipseView() {
	}

	public void init(Composite parent){
		taskService=new TaskServiceImpl(getViewSite());
		labelProvider=new TasklipseLabelProvider();
		taskFilter=new TaskFilter();
	}

	public void initComponents(Composite parent) throws CoreException{

		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

		composite=new Composite(tabFolder,SWT.None);
		GridLayout gl = new GridLayout(1,false);
		composite.setLayout(gl);
		initWidgets(composite);

		viewer=createTableViewer(composite);
		createTable();
		createColumns();

		//Setting content provider
		viewer.setContentProvider(new TasklipseContentProvider());

		//Setting Comparator to support Sorting
		comparator=new TasklipseTableComparator();
		viewer.setComparator(comparator);

		viewer.setFilters(new ViewerFilter[]{taskFilter});

		createTabs(tabFolder);

		tabFolder.setSelection(0);

		tabFolder.addSelectionListener(new SelectionAdapter() {
			String selected="Tasks";
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
				if(!tabFolder.getSelection()[0].getText().equals(selected)){
					selected=tabFolder.getSelection()[0].getText();
					refreshView(selected);
				}

			}
		});

		try {
			tasks=getTasks("Tasks");
			viewer.setInput(tasks);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refreshView(String selected) {

		tasks=new ArrayList<Task>();
		tasks=getTasks(selected);

		viewer.setInput(tasks);
		viewer.refresh();

	}

	private ArrayList<Task> getTasks(String selected){
		ArrayList<Task> taskList=new ArrayList<Task>();
		try{
			NullProgressMonitor monitor = new NullProgressMonitor();
			if (monitor.isCanceled())
				return taskList;
			monitor.beginTask("Fetching tasks", 10);
			taskList=taskService.getTaskListForAllProjects(selected);
			monitor.worked(10);
			monitor.done();

		} catch (CoreException e) {
			e.printStackTrace();
		}
		return taskList;
	}

	private void createTabs(TabFolder tabFolder) {
		TabItem tasksTab = new TabItem(tabFolder, SWT.NONE);
		tasksTab.setText("Tasks");
		tasksTab.setToolTipText("All active tasks");
		tasksTab.setControl(composite);

		TabItem archivedTab = new TabItem(tabFolder, SWT.NONE);
		archivedTab.setText("Archived");
		archivedTab.setToolTipText("All archived tasks");
		archivedTab.setControl(composite);
	}

	private CheckboxTableViewer createTableViewer(Composite parent) {


		viewer = CheckboxTableViewer.newCheckList(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL | SWT.CHECK);
		viewer.setUseHashlookup(true);
		viewer.setLabelProvider(labelProvider);
		//layout of the view
		//-----------------------------------------------------
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);
		//-----------------------------------------------------
		viewer.setColumnProperties(getColumnNames());

		viewer.addCheckStateListener(new ICheckStateListener()
		{
			@Override
			public void checkStateChanged(CheckStateChangedEvent event)
			{	
				Table table = (Table) viewer.getTable();
				TableItem item = table.getItem(table.getSelectionIndex());
				/*for (int col = 0; col < table.getColumnCount(); col++){
					if(item.getChecked())
						item.setChecked(false);
					else
						item.setChecked(true);  
				}*/
			}             
		});

		return viewer;

	}

	private String[] getColumnNames() {
		TaskColumn[] columns = TaskColumn.values();
		String[] names = new String[columns.length];

		for (int i = 0; i < columns.length; i++) {
			names[i] = columns[i].getDisplayName();
		}

		return names;

	}

	@Override  
	public void createPartControl(Composite parent) {  

		//InitializingView
		init(parent);

		//Initializing table
		try {
			initComponents(parent);
			if(TASKLIPSE_PROP_RESOURCE_CHANGE_LISTENER)
				addResourceChangeListener();
		} catch (CoreException e) {
			System.out.println("Error occurred while initializing table");
			e.printStackTrace();
		}
	}  


	private void initWidgets(Composite parent) {

		Composite actions=new Composite(parent,SWT.None);
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.fill = false;
		layout.justify = true;
		actions.setLayout(layout);

		Label searchLabel = new Label(actions, SWT.NONE| SWT.CENTER);
		searchLabel.setText("Search: ");
		final Text filterText = new Text(actions, SWT.BORDER | SWT.SEARCH);
		filterText.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ke) {
				taskFilter.setSearchText(filterText.getText());
				viewer.refresh();
			}
		});

		//Setting search text box width
		RowData data = new RowData();
		data.width=300;
		filterText.setLayoutData(data);

		//Adding archive button
		Button archive = new Button (actions, SWT.PUSH);
		archive.setText("Mark as Done");

		archive.addSelectionListener( new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
	
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				Object[] selection = viewer.getCheckedElements()!=null?viewer.getCheckedElements():null;
				if(selection!=null)
					taskService.archiveTasks(Arrays.asList(selection));
			}
		});

		//Adding delete button
		/*Button delete = new Button (actions, SWT.PUSH);
		delete.setText("Delete");

		delete.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					deleteSelectedTasks(selection.toList());
					break;
				}
			}
		});
		 */
	}

	private void addResourceChangeListener() {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		resourceChangedListener = new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					if (event.getType() != IResourceChangeEvent.POST_CHANGE)
						return;
					tasks=getTasks("Tasks");
				} catch (Exception e) {
					e.printStackTrace();
				}

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						viewer.setInput(tasks);
						viewer.refresh();
					}
				});
			}
		};
		workspace.addResourceChangeListener(resourceChangedListener);
	}	

	private void createTable(){
		table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}


	private void createColumns() {

		for(TaskColumn taskCol:TaskColumn.values()){

			if(taskCol.equals(TaskColumn.CB)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(false);
				column.setMoveable(false);
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return "";
					}
				});
			}


			if(taskCol.equals(TaskColumn.PRIORITY)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Task)element).getPriority()+"";
					}

					@Override
					public Color getBackground(final Object element) {
						if (element instanceof Task) {
							int priority=((Task) element).getPriority();
							if (priority==0) {
								return super.getBackground(element);
							}
							else if(priority==1){
								return new Color(Display.getDefault(), 0x72, 0xF0, 0x8D);	
							}
							else if(priority==2){
								return new Color(Display.getDefault(), 0xAF, 0xFD, 0xC0);	
							}
							else if(priority==3){
								return new Color(Display.getDefault(), 0xF6, 0xF6, 0x9A);	
							}
							else if(priority==4){
								return new Color(Display.getDefault(), 0xEA, 0xA1, 0x32);	
							}
							else if(priority>=5){
								return new Color(Display.getDefault(), 0xEC, 0x7A, 0x54);	
							}
						}
						return super.getBackground(element);
					}
				});

				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
			else if(taskCol.equals(TaskColumn.MESSAGE)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Task)element).getMessage();
					}
				});
				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
			else if(taskCol.equals(TaskColumn.RESOURCE)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);

				ColumnLabelProvider clp=new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Task)element).getTaskResource();
					}
				};

				col.setLabelProvider(new HyperLinkLabelProvider(clp));
				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
			else if(taskCol.equals(TaskColumn.TYPE)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Task)element).getTaskType();
					}
				});
				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
			else if(taskCol.equals(TaskColumn.CREATEDTIME)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);
				col.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Task)element).getCreatedTime();
					}
				});
				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
			else if(taskCol.equals(TaskColumn.ACTIONS)){
				TableViewerColumn col = new TableViewerColumn(viewer, SWT.RIGHT);
				final TableColumn column = col.getColumn();
				column.setText(taskCol.getDisplayName());
				column.setWidth(taskCol.getBounds());
				column.setResizable(true);
				column.setMoveable(true);

				col.setLabelProvider(new ColumnLabelProvider() {

					@Override
					public void update(ViewerCell cell) {
						TableItem item = (TableItem) cell.getItem();

						Composite buttonPane = new Composite(viewer.getTable(), SWT.NONE);

						buttonPane.setLayout(new FillLayout());

						Button button = new Button(buttonPane,SWT.NONE);
						button.setText("Done");
						button.setToolTipText("Mark as Done");
						button.setSize(20,20);
						button.addListener(SWT.Selection, new Listener(){

							@Override
							public void handleEvent(Event arg0) {
								taskService.archiveTask(cell.getElement());
							}
						});

						/*
						 * button = new Button(buttonPane,SWT.NONE);
						button.setText("X");
						button.setToolTipText("Delete Task");

						button.addListener(SWT.Selection, new Listener(){

							@Override
							public void handleEvent(Event arg0) {
								deleteTask(cell.getElement());
							}
						});
						 */

						TableEditor editor = new TableEditor(viewer.getTable());
						editor.grabHorizontal  = true;
						editor.grabVertical = true;
						editor.setEditor(buttonPane, item, taskCol.getColIndex());
						editor.layout();
					}

				});
				column.addSelectionListener(getSelectionAdapter(column, taskCol.getColIndex()));
			}
		}

	}


	private SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				viewer.getTable().setSortDirection(dir);
				viewer.getTable().setSortColumn(column);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}


	private TableViewerColumn createTableColumn(String title, int bound, final int colNumber) {
		TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
		final TableColumn column = col.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return col;
	}

	private static Image getImage(String file) {


		Bundle bundle = FrameworkUtil.getBundle(TasklipseView.class);
		URL url = FileLocator.find(bundle, new Path("icons/" + file), null);
		ImageDescriptor image = ImageDescriptor.createFromURL(url);
		return image.createImage();
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(
				getSite().getShell(),
				"Tasklipse",
				message);
	}


	public void setFocus() {
		viewer.getControl().setFocus();
	}


	public String getContributorId() {
		return ID;
	}

	public Object getAdapter(Class adapter) { 
		return super.getAdapter(adapter);
	}

	@Override
	public void dispose(){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(resourceChangedListener);
		viewer=null;
	}

	public class TasklipseContentProvider implements IStructuredContentProvider{

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			//This ensures that the row controls are disposed whenever the input changes
			if (((TableViewer)viewer).getTable() != null && ((TableViewer)viewer).getTable().getChildren() != null) {
				for (Control item : ((TableViewer)viewer).getTable().getChildren()) {
					if ((item != null) && (!item.isDisposed())) {  
						item.dispose();
					}
				}
			}
		}

		@Override
		public Object[] getElements(Object list) {
			return tasks.toArray();
		}

	}

}
