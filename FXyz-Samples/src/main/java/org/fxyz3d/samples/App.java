package org.fxyz3d.samples;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import de.javagl.obj.ObjWriter;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.MeshContainer;
import eu.mihosoft.jcsg.VFX3DUtil;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.fxyz3d.shapes.primitives.SpringMesh;
import org.fxyz3d.utils.CameraTransformer;
import org.json.JSONObject;
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArray;
import org.mapdb.serializer.SerializerArrayTuple;

/**
 * JavaFX App java -Xms6g -Xmx6g -Dprism.forceGPU=true -Dprism.verbose=true
 * -Dcom.sun.javafx.experimental.embedded.3d=true -Dprism.glDepthSize=24
 * -Dprism.order=sw
 * --module-path="D:\DevTools\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib"
 * --add-modules="javafx.base,javafx.controls" -cp
 * target\TgApp-1.0-SNAPSHOT-jar-with-dependencies.jar
 * com.truegeometry.tgapp.App java -Xms6g -Xmx6g -Dprism.order=sw
 * --module-path="D:\DevTools\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib"
 * --add-modules="javafx.base,javafx.controls" -cp TgApp-1.0-SNAPSHOT.jar
 * com.truegeometry.tgapp.App
 *
 */
public class App extends Application {

    private CSG csgObject;
    VBox rootContainer = new VBox();
    Pane viewContainer = new Pane();
    Group viewGroup = new Group();
    CSG baseObject;

    //Storage
    DB db;
    public HTreeMap<String, String> store;
    public Map<String, byte[]> sessionStore;
    public BTreeMap<Object[], String> index;
    Atomic.Var<String> TGAPPKey ;
    Atomic.Var<String> TGServer ;
    int CurrentPageIndex = 0;

    @Override
    public void start(Stage primaryStage) throws Exception {

        File selectedFile = pickFolder("Select Working Directory");//fileChooser.showOpenDialog(null);
        db = DBMaker
                .fileDB(selectedFile + "/" + "CachePreview.db").transactionEnable()
                .closeOnJvmShutdown().cleanerHackEnable().fileChannelEnable().fileMmapEnableIfSupported()
                .make();
        store = db.hashMap("store")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.STRING)
                .createOrOpen();

        sessionStore = db.hashMap("sessionStore", Serializer.STRING, Serializer.BYTE_ARRAY_NOSIZE).expireAfterGet(30, TimeUnit.DAYS) //Expire content in 365 days
                .createOrOpen();

        index = db.treeMap("index")
                // use array serializer for unknown objects
                .keySerializer(new SerializerArrayTuple(
                        Serializer.STRING, Serializer.STRING, Serializer.STRING))
                // or use wrapped serializer for specific objects such as String
                .keySerializer(new SerializerArray(Serializer.STRING))
                .createOrOpen();//Issue with keys may be...
        
        TGAPPKey = db.atomicVar("TGAPPKey",Serializer.STRING).createOrOpen();
        TGServer = db.atomicVar("TGServer",Serializer.STRING).createOrOpen();
        TGServer.set("http://www.truegeometry.com");
        //TGServer.set("http://localhost:8086");

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateX(10);
        camera.setTranslateZ(-100);
        camera.setFieldOfView(20);

        CameraTransformer cameraTransform = new CameraTransformer();
        cameraTransform.getChildren().add(camera);
        cameraTransform.ry.setAngle(-30.0);
        cameraTransform.rx.setAngle(-15.0);

        VBox userControls = loadUserControls();//new VBox(menuBar,listView);
        viewContainer.getChildren().add(userControls);
        //viewContainer.getChildren().add(listView);

        //setMeshScale(meshContainer,  viewContainer.getBoundsInLocal(), meshView);
        SubScene subScene = new SubScene(viewGroup, 100, 100, true,
                SceneAntialiasing.BALANCED);

        subScene.widthProperty().bind(viewContainer.widthProperty());
        subScene.heightProperty().bind(viewContainer.heightProperty());

        PerspectiveCamera subSceneCamera = new PerspectiveCamera(false);
        subScene.setCamera(subSceneCamera);

        viewContainer.getChildren().add(subScene);

        //Group group = new Group(cameraTransform, meshView);
        Scene scene = new Scene(viewContainer, 600, 400, true, SceneAntialiasing.BALANCED);
        // scene.setFill(Color.BISQUE);
        //scene.setCamera(camera);

        primaryStage.setScene(scene);
        primaryStage.setTitle("FXyz3D | www.truegeometry.com | AI Powered 3D model management");
        primaryStage.show();
    }

    private VBox loadUserControls() {
        
         // Create the TreeView
        TreeView treeView = new TreeView();
        // Create the Root TreeItem
        TreeItem rootItem = new TreeItem("Session");

        ListView listView = new ListView();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    System.out.println("clicked on " + listView.getSelectionModel().getSelectedItem());
                    try {
                        String selectedItem = (String) listView.getSelectionModel().getSelectedItem();
                        Obj obj = null;
                        if (sessionStore.containsKey(selectedItem)) {
                            byte[] objData = sessionStore.get(selectedItem);
                            ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                            obj = ObjReader.read(byt);
                        } else {

                            obj = ObjReader.read(new File(selectedItem).toURI().toURL().openStream());
                        }

                        CSG objCSG = OBJCSGHelper.toCSG(obj);
                        load(objCSG);

                    } catch (MalformedURLException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        //Load previous session
        sessionStore.keySet().forEach(kv -> {
            listView.getItems().add(kv);
        });

        Button button = new Button("TG Assembly Import");

        button.setOnAction(value -> {
            try {
                String filename="FILE-1610341173973.obj";
                String metaInfo = HTTPHelper.httpGetResponse(TGServer.get()+"/api/get3DAsm?modelName="+filename+"&APIKey=t0UcU+uE8kkV8EiccWpBcSweJbSlyXxy");
                JSONObject resp = new JSONObject(metaInfo);
                List<DownloadTask> downloadlist = new LinkedList<>();
                Set<String> parts = resp.getJSONObject("parts").keySet();
                TreeItem rootItemC = new TreeItem(filename);
                for (String key : parts) {
                    DownloadTask dt = new DownloadTask(TGServer.get() + resp.getJSONObject("parts").getString(key) + "&APIKey=t0UcU+uE8kkV8EiccWpBcSweJbSlyXxy", sessionStore, key);
                    downloadlist.add(dt);
               
                    TreeItem thisItem = new TreeItem(key);
                    rootItemC.getChildren().add(thisItem);
 
                }
                rootItem.getChildren().add(rootItemC);

                new ProgressDialogProd(downloadlist);
                
                            
             
            
            
                db.commit();
                
                 

            } catch (MalformedURLException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

//             SpringMesh spring = new SpringMesh(10, 2, 2, 8 * 2 * Math.PI, 200, 100, 0, 0);
//             spring.setCullFace(CullFace.NONE);
//             spring.setTextureModeVertices3D(1530, p -> p.f);
        //Creating a menu
        Menu fileMenu = new Menu("File");
        //Creating a menu bar and adding menu to it.
        MenuBar menuBar = new MenuBar(fileMenu);
        //Creating menu Items
        MenuItem item = new MenuItem("Load Model");
        fileMenu.getItems().addAll(item);

        //Creating a File chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Model");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));
        //fileChooser.showOpenDialog(primaryStage);
        //Adding action on the menu item
        item.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                //Opening a dialog box
                // fileChooser.showOpenDialog(primaryStage);

                try {
                    File objFile = pickFile("Pick Obj File", null);
                    //l1.addElement(objFile.getAbsolutePath());
                    Obj obj = ObjReader.read(objFile.toURI().toURL().openStream());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    //  Obj obj = ObjReader.read(new File("D:/Downloads/269-TGCMP-0.obj").toURI().toURL().openStream());
                    CSG objCSG = OBJCSGHelper.toCSG(obj);
                    load(objCSG);

                    //Also cache it.
                    ObjWriter.write(obj, stream);
                    sessionStore.put(objFile.getName(), stream.toByteArray());
                    db.commit();

                    listView.getItems().add(objFile.getName());

                    //reset(new Object3D(ObjUtils.triangulate(obj), true));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        MenuItem TGImport = new MenuItem("TG Assembly Import");
        fileMenu.getItems().addAll(TGImport);
        TGImport.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {

                try {

                    // create a text input dialog
                    TextInputDialog td = new TextInputDialog("Enter TG file name");

                    // setHeaderText
                    td.setHeaderText("www.truegeometry.com");
                    td.showAndWait();
                    String filename = td.getEditor().getText();//"FILE-1609843089509.obj";
                    String metaInfo = HTTPHelper.httpGetResponse(TGServer.get()+"/api/get3DAsm?modelName=" + filename + "&APIKey="+TGAPPKey.get());
                    JSONObject resp = new JSONObject(metaInfo);
                    List<DownloadTask> downloadlist = new LinkedList<>();
                    Set<String> parts = resp.getJSONObject("parts").keySet();
                    TreeItem rootItemC = new TreeItem(filename);

                    for (String key : parts) {
                        DownloadTask dt = new DownloadTask(TGServer.get() + resp.getJSONObject("parts").getString(key) + "&APIKey="+TGAPPKey.get(), sessionStore, key);
                        downloadlist.add(dt);
                        TreeItem thisItem = new TreeItem(key);
                        rootItemC.getChildren().add(thisItem);
                    }
                    rootItem.getChildren().add(rootItemC);

                    new ProgressDialogProd(downloadlist);

                    db.commit();


                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        MenuItem TGAPIKey = new MenuItem("Set TG API Key");
        fileMenu.getItems().addAll(TGAPIKey);
        TGAPIKey.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                    // create a text input dialog
                    TextInputDialog td = new TextInputDialog(TGAPPKey.get()!=null?TGAPPKey.get():"Enter TG API Key");
                    // setHeaderText
                    td.setHeaderText("www.truegeometry.com");
                    td.showAndWait();
                    String filename = td.getEditor().getText();
                    TGAPPKey.set(filename);
                    db.commit();
            }
        });
        
        
        MenuItem TGCImport = new MenuItem("TG Crossover Import");
        fileMenu.getItems().addAll(TGCImport);
        TGCImport.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                try {
                    int m1 = 522;
                    int m2 = 311;
                    String fileName = "TGC-"+m1 + "-" + m2;
                    String metaInfo = HTTPHelper.httpGetResponse(TGServer.get()+"/api/transform/model3D?m1=522&m2=311&APIKey="+TGAPPKey.get());
                    JSONObject resp = new JSONObject(metaInfo);
                    List<DownloadTask> downloadlist = new LinkedList<>();
                    Set<String> parts = resp.keySet();
                    TreeItem rootItemC = new TreeItem(fileName);

                    for (String key : parts) {
                        DownloadTask dt = new DownloadTask(TGServer.get() + resp.getString(key) + "&APIKey="+TGAPPKey.get(), sessionStore, "C" + key);
                        downloadlist.add(dt);
                        TreeItem thisItem = new TreeItem("C" + key);
                        rootItemC.getChildren().add(thisItem);
                    }
                    rootItem.getChildren().add(rootItemC);

                    new ProgressDialogProd(downloadlist);
                    db.commit();

                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        //+++++++++++CSG OPERATIONS 
        Button btnJoin = new Button("Join Selected");
        btnJoin.setOnAction(value -> {

            List<String> selected = listView.getSelectionModel().getSelectedItems();
            List<CSG> selectedCSG = new LinkedList<>();

            for (String objFilePath : selected) {
                try {
                    Obj obj = null;
                    if (sessionStore.containsKey(objFilePath)) {
                        byte[] objData = sessionStore.get(objFilePath);
                        ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                        obj = ObjReader.read(byt);
                    } else {

                        obj = ObjReader.read(new File(objFilePath).toURI().toURL().openStream());
                    }
                    selectedCSG.add(OBJCSGHelper.toCSG(obj));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

             baseObject = selectedCSG.get(0);

            for (int i = 1; i < selectedCSG.size(); i++) {
                baseObject = baseObject.dumbUnion(selectedCSG.get(i));
            }

            load(baseObject);
        });

        Button btnDifference = new Button("Difference Selected");
        btnDifference.setOnAction(event -> {

            List<String> selected = listView.getSelectionModel().getSelectedItems();
            List<CSG> selectedCSG = new LinkedList<>();

            for (String objFilePath : selected) {
                try {
                    Obj obj = null;
                    if (sessionStore.containsKey(objFilePath)) {
                        byte[] objData = sessionStore.get(objFilePath);
                        ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                        obj = ObjReader.read(byt);
                    } else {

                        obj = ObjReader.read(new File(objFilePath).toURI().toURL().openStream());
                    }
                    selectedCSG.add(OBJCSGHelper.toCSG(obj));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            baseObject = selectedCSG.get(0);

            for (int i = 1; i < selectedCSG.size(); i++) {
                baseObject = baseObject.difference(selectedCSG.get(i));
            }

            load(baseObject);
        });

        Button btnIntersect = new Button("Intersect Selected");
        btnIntersect.setOnAction(event -> {

            List<String> selected = listView.getSelectionModel().getSelectedItems();
            List<CSG> selectedCSG = new LinkedList<>();

            for (String objFilePath : selected) {
                try {
                    Obj obj = null;
                    if (sessionStore.containsKey(objFilePath)) {
                        byte[] objData = sessionStore.get(objFilePath);
                        ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                        obj = ObjReader.read(byt);
                    } else {

                        obj = ObjReader.read(new File(objFilePath).toURI().toURL().openStream());
                    }
                    selectedCSG.add(OBJCSGHelper.toCSG(obj));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            baseObject = selectedCSG.get(0);

            for (int i = 1; i < selectedCSG.size(); i++) {
                baseObject = baseObject.intersect(selectedCSG.get(i));
            }

            load(baseObject);
        });

        //Reset button
        Button btnSave = new Button("Save CSG");
        btnSave.setOnAction(event -> {
            try {
                Obj toSave = OBJCSGHelper.toObj(baseObject);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                //Also cache it.
                ObjWriter.write(toSave, stream);
                String newFileName = "CSG-" + System.currentTimeMillis() + ".obj";
                sessionStore.put(newFileName, stream.toByteArray());
                db.commit();
                listView.getItems().add(newFileName);
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            db.commit();
        });

        Button btnExport = new Button("Export Selected");
        btnExport.setOnAction(event -> {
            String selectedItem = (String) listView.getSelectionModel().getSelectedItem();
            Obj obj = null;
            if (sessionStore.containsKey(selectedItem)) {
                try {
                    byte[] objData = sessionStore.get(selectedItem);
                    ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                    obj = ObjReader.read(byt);

                    // Write an OBJ file
                    File folderToExport = pickFolder("Folder to export");
                    OutputStream objOutputStream = new FileOutputStream(folderToExport.getAbsolutePath() + "/TG-" + System.currentTimeMillis() + ".obj");
                    ObjWriter.write(obj, objOutputStream);
                    objOutputStream.flush();
                    objOutputStream.close();

                    int input = JOptionPane.showConfirmDialog(null, "Exported", "OK", JOptionPane.OK_OPTION);

                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
         Button btnUpload = new Button("Upload Selected");
        btnUpload.setOnAction(event -> {
            String selectedItem = (String) listView.getSelectionModel().getSelectedItem();
            Obj obj = null;
            if (sessionStore.containsKey(selectedItem)) {
                try {

                    byte[] objData = sessionStore.get(selectedItem);
                    
                    ByteArrayInputStream byt = new ByteArrayInputStream(objData);

                    String tag = JOptionPane.showInputDialog("Enter tag");
                    String[] geometryClass = {"Building", "MegaStructures", "Vehicles", "Ships", "Characters", "Aircraft", "Furniture", "Electronics", "Animals", "Plants", "Weapons", "Sports", "Food", "Anatomy", "Other", "OuterSpace"};
                    JComboBox petList = new JComboBox(geometryClass);
                    String[] options = {"OK"};

                    int selection = JOptionPane.showOptionDialog(null, petList, "Select Class:",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                            options, options[0]);

                    Object weekday = petList.getSelectedItem();
                    HashMap<String, String> params = new HashMap<>();
                    params.put("tags", tag);
                    params.put("GeometryClass", weekday.toString());
                    
                    String jsoKeys = HTTPHelper.httpGetResponse(TGServer.get() + "/getUpldToken?APIKey=" + TGAPPKey.get());
                    JSONObject resp = new JSONObject(jsoKeys);
                    String apiKeyToken = resp.getString("apiKeyToken");

                    if (objData.length < 1024 * 1024 * 2) { //less than 2MB size

                        String urlString = TGServer.get() + "/upldAPI?APIKey=" + TGAPPKey.get() + "&APIKeyToken=" + apiKeyToken + "&tags=" + HTTPHelper.encodeValue(tag) + "&GeometryClass=" + weekday.toString();
                        int status = HTTPHelper.uploadFile(urlString, selectedItem, byt, params);
                        int input = JOptionPane.showConfirmDialog(null, "Upload Status" + status, "OK", JOptionPane.OK_OPTION);

                    } else {
                        ByteBuffer bb = ByteBuffer.wrap(objData);
                        int chunkSize=1024 * 1024 * 1;
                        
                        for (int i = 0,chunkCOunt=0; i < objData.length; i = i +chunkSize,chunkCOunt++ ) {//Chunk of 1MB
                            boolean lastChunk=i+chunkSize >= objData.length;
                            byte[] chunk=new byte[lastChunk?(objData.length-i):chunkSize];
                            System.out.println("Chunk Sizes: Full>>"+objData.length+" Chunk>> "+ i + " Allocated>>"+chunk.length);
                            bb.get(chunk, 0, chunk.length);
                            params.put("chunkLast", String.valueOf(lastChunk));
                            params.put("chunkSeq",  String.valueOf(chunkCOunt));
                            params.put("fileName",  selectedItem);
 
                            String urlString = TGServer.get() + "/upldChunkAPI?APIKey=" + TGAPPKey.get() + "&APIKeyToken=" + apiKeyToken +
                                    "&tags=" + HTTPHelper.encodeValue(tag) 
                                    + "&GeometryClass=" + weekday.toString()
                                    + "&chunkLast="+String.valueOf(lastChunk)
                                    +"&chunkSeq="+String.valueOf(chunkCOunt)
                                    +"&fileName="+selectedItem;
                            int status = HTTPHelper.uploadFile(urlString, selectedItem,  new ByteArrayInputStream(chunk), params);
                        }

                    }

                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        Button btnRefreshSession = new Button("Session Refresh");
        btnRefreshSession.setOnAction(event -> {
            db.commit();
            listView.getItems().clear();
            sessionStore.keySet().forEach(kv -> {
                listView.getItems().add(kv);
            });             
        });

        //Reset button
        Button btnResetSession = new Button("Session Reset");
        btnResetSession.setOnAction(event -> {
            sessionStore.clear();
            db.commit();
            listView.getItems().clear();
        });
        

        treeView.setRoot(rootItem);
        //Action on tree
        treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue,
                    Object newValue) {

                TreeItem<String> selectedItem = (TreeItem<String>) newValue;
                System.out.println("Selected Text : " + selectedItem.getValue());

                if (selectedItem.isLeaf()) {//If this is leaf node
                    try {
                        Obj obj = null;
                        if (sessionStore.containsKey(selectedItem.getValue())) {
                            byte[] objData = sessionStore.get(selectedItem.getValue());
                            ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                            obj = ObjReader.read(byt);
                        } else {
                            obj = ObjReader.read(new File(selectedItem.getValue()).toURI().toURL().openStream());
                        }
                        CSG objCSG = OBJCSGHelper.toCSG(obj);
                        load(objCSG);
                    } catch (IOException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    if (selectedItem.getParent() != null) {
                        baseObject = null;
                        List<CSG> selectedCSG = new LinkedList<>();
                        selectedItem.getChildren().forEach(child -> {
                            try {
                                Obj obj = null;
                                if (sessionStore.containsKey(child.getValue())) {
                                    byte[] objData = sessionStore.get(child.getValue());
                                    ByteArrayInputStream byt = new ByteArrayInputStream(objData);
                                    obj = ObjReader.read(byt);
                                } else {
                                    obj = ObjReader.read(new File(child.getValue()).toURI().toURL().openStream());
                                }
                                selectedCSG.add(OBJCSGHelper.toCSG(obj));
                            } catch (MalformedURLException ex) {
                                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });

                        baseObject = selectedCSG.get(0);
                        for (int i = 1; i < selectedCSG.size(); i++) {
                            baseObject = baseObject.dumbUnion(selectedCSG.get(i));
                        }
                        
                        //Views
                        load(baseObject);
                        
                        //Cache it 
                        save(selectedItem.getValue());
                        
                        //Refresh view
                        listView.getItems().clear();
                        sessionStore.keySet().forEach(kv -> {
                            listView.getItems().add(kv);
                        });

                    }

                }
                // do what ever you want 
            }

        });
         
         
        
        VBox vBox = new VBox(menuBar, btnRefreshSession,treeView, listView, btnJoin, btnDifference, btnIntersect, btnSave, btnExport,btnUpload, btnResetSession);
        vBox.setPrefWidth(200);

        btnRefreshSession.setMinWidth(vBox.getPrefWidth());
        btnJoin.setMinWidth(vBox.getPrefWidth());
        btnDifference.setMinWidth(vBox.getPrefWidth());
        btnIntersect.setMinWidth(vBox.getPrefWidth());
        btnExport.setMinWidth(vBox.getPrefWidth());
        btnResetSession.setMinWidth(vBox.getPrefWidth());
        btnSave.setMinWidth(vBox.getPrefWidth());
        btnUpload.setMinWidth(vBox.getPrefWidth());
        listView.setMinWidth(vBox.getPrefWidth());
        
        btnRefreshSession.setStyle("  -fx-background-color: \n" +
"        #ecebe9,\n" +
"        rgba(0,0,0,0.05),\n" +
"        linear-gradient(#dcca8a, #c7a740),\n" +
"        linear-gradient(#f9f2d6 0%, #f4e5bc 20%, #e6c75d 80%, #e2c045 100%),\n" +
"        linear-gradient(#f6ebbe, #e6c34d);\n" +
"    -fx-background-insets: 0,9 9 8 9,9,10,11;\n" +
"    -fx-background-radius: 50;\n" +
"    -fx-padding: 15 30 15 30;\n" +
"    -fx-font-family: \"Helvetica\";\n" +
"    -fx-font-size: 18px;\n" +
"    -fx-text-fill: #311c09;\n" +
"    -fx-effect: innershadow( three-pass-box , rgba(0,0,0,0.1) , 2, 0.0 , 0 , 1);");

 //http://fxexperience.com/2011/12/styling-fx-buttons-with-css/


        return vBox;//new VBox(menuBar,btnRefreshSession,listView,btnJoin,btnDifference,btnIntersect,btnSave,btnExport,btnResetSession);

    }

    private void load(CSG csg) {

        MeshContainer meshContainer = csg.toJavaFXMesh();

        final MeshView meshView = meshContainer.getAsMeshViews().get(0);

        //Clear
        viewGroup.getChildren().clear();

        meshView.setCullFace(CullFace.NONE);
        PhongMaterial m = new PhongMaterial(Color.BLUE);

        meshView.setMaterial(m);

        viewGroup.layoutXProperty().bind(viewContainer.widthProperty().divide(2));
        viewGroup.layoutYProperty().bind(viewContainer.heightProperty().divide(2));

        viewContainer.boundsInLocalProperty().addListener(
                (ov, oldV, newV) -> {
                    setMeshScale(meshContainer, newV, meshView);
                });

        VFX3DUtil.addMouseBehavior(meshView,
                viewContainer, MouseButton.PRIMARY);

        viewGroup.getChildren().add(meshView);
    }
    
   /**
    * 
    * @param keyNameObj 
    */
    private void save(String keyNameObj) {
        //Also cache it
        try {
            Obj toSave = OBJCSGHelper.toObj(baseObject);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            //Also cache it.
            ObjWriter.write(toSave, stream);
            String newFileName = keyNameObj;// "CSG-" + System.currentTimeMillis() + ".obj";
            sessionStore.put(newFileName, stream.toByteArray());
            db.commit();

        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

        db.commit();
    }
    
    
    public static void main(String[] args) {
        System.out.println("java -Xms6g -Xmx6g -Dprism.order=sw --module-path=\"D:\\DevTools\\openjfx-11.0.2_windows-x64_bin-sdk\\javafx-sdk-11.0.2\\lib\" --add-modules=\"javafx.base,javafx.controls\" -cp TgApp-1.0-SNAPSHOT.jar com.truegeometry.tgapp.App");
        launch();
    }

    private void compile(CSG csg) {

        csgObject = null;

        viewGroup.getChildren().clear();

        try {

            // Object obj = script.run();
            if (csg instanceof CSG) {

                //   CSG csg = (CSG) obj;
                csgObject = csg;

                MeshContainer meshContainer = csg.toJavaFXMesh();

                final MeshView meshView = meshContainer.getAsMeshViews().get(0);

                setMeshScale(meshContainer,
                        viewContainer.getBoundsInLocal(), meshView);

                PhongMaterial m = new PhongMaterial(Color.RED);

                meshView.setCullFace(CullFace.NONE);

                meshView.setMaterial(m);

                viewGroup.layoutXProperty().bind(
                        viewContainer.widthProperty().divide(2));
                viewGroup.layoutYProperty().bind(
                        viewContainer.heightProperty().divide(2));

                viewContainer.boundsInLocalProperty().addListener(
                        (ov, oldV, newV) -> {
                            setMeshScale(meshContainer, newV, meshView);
                        });

                VFX3DUtil.addMouseBehavior(meshView,
                        viewContainer, MouseButton.PRIMARY);

                viewGroup.getChildren().add(meshView);

            } else {
                System.out.println(">> no CSG object returned :(");
            }

        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
        }
    }

    private void setMeshScale(MeshContainer meshContainer, Bounds t1, final MeshView meshView) {
        double maxDim = Math.max(meshContainer.getWidth(), Math.max(meshContainer.getHeight(), meshContainer.getDepth()));

        double minContDim = Math.min(t1.getWidth(), t1.getHeight());

        double scale = minContDim / (maxDim * 2);

        meshView.setScaleX(scale);
        meshView.setScaleY(scale);
        meshView.setScaleZ(scale);
    }

    public static File pickFolder(String title) {
        String choosertitle;
        JFileChooser chooser;
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        //    
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "
                    + chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "
                    + chooser.getSelectedFile());

            return chooser.getSelectedFile();
        } else {
            System.out.println("No Selection ");
        }
        return null;

    }

    public static File pickFile(String title, File hostDirectory) {
        String choosertitle;
        JFileChooser chooser;
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(hostDirectory == null ? new java.io.File(".") : hostDirectory);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        //    
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "
                    + chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "
                    + chooser.getSelectedFile());

            return chooser.getSelectedFile();
        } else {
            System.out.println("No Selection ");
        }
        return null;

    }

}
