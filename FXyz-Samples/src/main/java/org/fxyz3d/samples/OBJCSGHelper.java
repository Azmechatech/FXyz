/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxyz3d.samples;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjFaces;
import de.javagl.obj.ObjUtils;
import de.javagl.obj.Objs;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.jcsg.Vertex;
import eu.mihosoft.vvecmath.Vector3d;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ryzen
 */
public class OBJCSGHelper {
    
    public static CSG toCSG(Obj objInput) throws IOException {
        Obj obj = ObjUtils.triangulate(objInput); // Triangulation is important to ensure basic polygon construction.
        ArrayList<Vector3d> verticesObj = new ArrayList<>();

//        for (int i = 0; i < obj.getNumVertices(); i++) {
//            vertData.append(obj.getVertex(i).getX()).append(" ").append(obj.getVertex(i).getY()).append(" ").append(obj.getVertex(i).getZ()).append(" ");
//        }
        for (int i = 0; i < obj.getNumFaces(); i++) {
            ObjFace obf = obj.getFace(i);
            for (int j = 0; j < obf.getNumVertices(); j++) {
                int vert = obf.getVertexIndex(j);
                //faces.append(vert).append(" ");

                float x = obj.getVertex(vert).getX();
                float y = obj.getVertex(vert).getY();
                float z = obj.getVertex(vert).getZ();
                Vector3d vertex = Vector3d.xyz(x, y, z);
                verticesObj.add(vertex);

            }

        }

        List<Polygon> polygons = new ArrayList<>();
        List<Vector3d> vertices = new ArrayList<>();

        for (Vector3d p : verticesObj) {
            vertices.add(p.clone());
            if (vertices.size() == 3) {
                polygons.add(Polygon.fromPoints(vertices));
                vertices = new ArrayList<>();
            }
        }

        return CSG.fromPolygons(new PropertyStorage(), polygons);
    }
    
    /**
     * 
     * @param csgInput
     * @return 
     */
    public static Obj toObj(CSG csgInput) {
        Obj newObj = Objs.create();
        List<Polygon> polygons = csgInput.getPolygons();
        long counter = 0;
        for (int i = 0; i < polygons.size(); i++) {
            Polygon p = polygons.get(i);
            List<Vertex> vertices = p.vertices;
            int vertexIndices[] = new int[vertices.size()];
            for (int j = 0; j < vertices.size(); j++) {
                Vertex v = vertices.get(j);
                newObj.addVertex((float) v.pos.getX(), (float) v.pos.getY(), (float) v.pos.getZ());
                vertexIndices[j] = (int) counter;
                counter++;
            }
            ObjFace ofNew = ObjFaces.create(vertexIndices, null, null);
            newObj.addFace(ofNew);

        }
        return newObj;
    }

    
}
