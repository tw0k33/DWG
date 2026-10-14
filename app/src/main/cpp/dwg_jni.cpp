#include <jni.h>
#include <vector>
#include <cstring>
#include <cmath>
extern "C" {
#include <dwg.h>
#include <dwg_api.h>
}

// Формат выходного массива (float), записи идут подряд:
// 1 LINE      : x1 y1 x2 y2
// 2 CIRCLE    : cx cy r
// 3 ARC       : cx cy r startRad endRad
// 4 POLYLINE  : closed(0/1) n x0 y0 ... x(n-1) y(n-1)

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_dwgviewer_DwgNative_readGeometry(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    Dwg_Data dwg;
    memset(&dwg, 0, sizeof(dwg));
    int err = dwg_read_file(path, &dwg);
    env->ReleaseStringUTFChars(jpath, path);

    // err >= DWG_ERR_CRITICAL (>= 128) — файл прочитать не удалось
    if (err >= DWG_ERR_CRITICAL) {
        dwg_free(&dwg);
        return nullptr;
    }

    std::vector<float> out;
    out.reserve(1 << 16);

    for (BITCODE_BL i = 0; i < dwg.num_objects; i++) {
        Dwg_Object *obj = &dwg.object[i];
        if (obj->supertype != DWG_SUPERTYPE_ENTITY || !obj->tio.entity) continue;
        Dwg_Object_Entity *ent = obj->tio.entity;

        switch (obj->fixedtype) {
        case DWG_TYPE_LINE: {
            Dwg_Entity_LINE *e = ent->tio.LINE;
            if (!e) break;
            out.insert(out.end(), {1.f, (float)e->start.x, (float)e->start.y,
                                         (float)e->end.x, (float)e->end.y});
            break;
        }
        case DWG_TYPE_CIRCLE: {
            Dwg_Entity_CIRCLE *e = ent->tio.CIRCLE;
            if (!e) break;
            out.insert(out.end(), {2.f, (float)e->center.x, (float)e->center.y, (float)e->radius});
            break;
        }
        case DWG_TYPE_ARC: {
            Dwg_Entity_ARC *e = ent->tio.ARC;
            if (!e) break;
            out.insert(out.end(), {3.f, (float)e->center.x, (float)e->center.y, (float)e->radius,
                                   (float)e->start_angle, (float)e->end_angle});
            break;
        }
        case DWG_TYPE_LWPOLYLINE: {
            Dwg_Entity_LWPOLYLINE *e = ent->tio.LWPOLYLINE;
            if (!e || !e->points || e->num_points < 2) break;
            out.push_back(4.f);
            out.push_back((e->flag & 512) ? 1.f : 0.f);
            out.push_back((float)e->num_points);
            for (BITCODE_BL k = 0; k < e->num_points; k++) {
                out.push_back((float)e->points[k].x);
                out.push_back((float)e->points[k].y);
            }
            break;
        }
        default: break;
        }
    }

    dwg_free(&dwg);

    jfloatArray arr = env->NewFloatArray((jsize)out.size());
    env->SetFloatArrayRegion(arr, 0, (jsize)out.size(), out.data());
    return arr;
}
