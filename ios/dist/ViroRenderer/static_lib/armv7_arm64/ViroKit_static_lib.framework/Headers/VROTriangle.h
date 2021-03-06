//
//  VROTriangle.h
//  ViroRenderer
//
//  Created by Raj Advani on 10/15/15.
//  Copyright © 2016 Viro Media. All rights reserved.
//

#ifndef VROTRIANGLE_H_
#define VROTRIANGLE_H_

#include "VROVector3f.h"
#include <sstream>

class VROMatrix4f;

class VROTriangle {
public:

    /*
     Construct a new triangle.
     */
    VROTriangle(VROVector3f a, VROVector3f b, VROVector3f c);
    virtual ~VROTriangle();
    
    /*
     Get the points of the triangle.
     */
    VROVector3f getA() const { return _a; }
    VROVector3f getB() const { return _b; }
    VROVector3f getC() const { return _c; }

    /*
     Return true if this triangle is degenerate, meaning two of its vertices
     are exactly equal.
     */
    bool isDegenerate() const;

    /*
     Return the vertex with the given index from [0,2]. 0=A, 1=B, 2=C.
     */
    VROVector3f vertexWithIndex(int index) const;

    /*
     Determine if the triangle intersects the given ray.
     */
    bool intersectsRay(VROVector3f ray, VROVector3f origin, VROVector3f *intPt) const;

    /*
     Determine if the triangle contains the given point.
     */
    bool containsPoint(VROVector3f p) const;

    /*
     Retrieve the barycenter of this triangle.
     */
    VROVector3f barycenter() const;

    /*
     Transform this triangle by the given 4x4 matrix.
     */
    VROTriangle transformByMatrix(VROMatrix4f matrix) const;
    
    std::string toString() const {
        std::stringstream ss;
        ss << std::fixed << "A: " << _a.x << ", " << _a.y << ", " << _a.z <<
                          ", B: " << _b.x << ", " << _b.y << ", " << _b.z <<
                          ", C: " << _c.x << ", " << _c.y << ", " << _c.z;
        
        return ss.str();
    }
    
private:
    
    /*
     Corners of the triangle (each length 3).
     */
    VROVector3f _a;
    VROVector3f _b;
    VROVector3f _c;
    
    /*
     Used for intersection (and containment) testing.
     */
    VROVector3f _segAB;
    VROVector3f _segBC;
    VROVector3f _segCA;
    VROVector3f _normal;
    
};

#endif /* VROTRIANGLE_H_ */
