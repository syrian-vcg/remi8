/**
 * ─────────────────────────────────────────────────
 *  REMI8 - remiscript Engine Core (C++)
 *  محرك remiscript الأساسي بلغة C++
 *  JNI Bridge للاتصال مع Java/Android
 * ─────────────────────────────────────────────────
 */

#include <jni.h>
#include <string>
#include <map>
#include <vector>
#include <cmath>
#include <sstream>
#include <functional>
#include <memory>
#include <stdexcept>
#include <android/log.h>

#define LOG_TAG "REMI8_CPP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ─── Script Graph Node Types ───────────────────────────────────────────────

enum class NodeType {
    EVENT_ON_UPDATE,    // On Update حدث
    EVENT_ON_START,     // On Start حدث
    MATH_ABS,           // Mathf.Abs
    MATH_MIN,           // Mathf.Min
    MATH_MAX,           // Mathf.Max
    MATH_SQRT,          // Mathf.Sqrt
    MATH_SIN,           // Mathf.Sin
    MATH_COS,           // Mathf.Cos
    FLOAT_VALUE,        // Float ثابت
    INT_VALUE,          // Int ثابت
    BOOL_VALUE,         // Bool ثابت
    STRING_VALUE,       // String ثابت
    OP_SUBTRACT,        // Subtract ناقص
    OP_ADD,             // Add زائد
    OP_MULTIPLY,        // Multiply ضرب
    OP_DIVIDE,          // Divide قسمة
    OP_COMPARE_GT,      // Greater Than أكبر
    OP_COMPARE_LT,      // Less Than أصغر
    BRANCH,             // Branch تفرع
    SET_VARIABLE,       // Set Variable تعيين متغير
    GET_VARIABLE,       // Get Variable قراءة متغير
    PRINT,              // Print طباعة
    CUSTOM,             // Custom مخصص
};

// ─── Script Graph Node ──────────────────────────────────────────────────────

struct ScriptNode {
    int id;
    NodeType type;
    float x, y;             // موقع في Graph
    float width, height;    // حجم

    // قيم الـ pins
    std::map<std::string, float>       floatValues;
    std::map<std::string, std::string> stringValues;

    // اتصالات: pinName → (nodeId, pinName)
    std::map<std::string, std::pair<int, std::string>> inputConnections;

    // label
    std::string label;
    std::string category;
};

// ─── Script Graph ────────────────────────────────────────────────────────────

class ScriptGraph {
public:
    std::map<int, ScriptNode> nodes;
    int nextId = 1;

    int addNode(NodeType type, float x, float y) {
        ScriptNode node;
        node.id = nextId++;
        node.type = type;
        node.x = x;
        node.y = y;
        node.width = 140;
        node.height = 80;

        // تهيئة القيم الافتراضية حسب النوع
        switch (type) {
            case NodeType::FLOAT_VALUE:
                node.floatValues["value"] = 0.0f;
                node.label = "Float";
                node.category = "Value";
                break;
            case NodeType::MATH_ABS:
                node.label = "Mathf Abs";
                node.category = "Math";
                break;
            case NodeType::MATH_MIN:
                node.label = "Mathf Min";
                node.category = "Math";
                break;
            case NodeType::MATH_MAX:
                node.label = "Mathf Max";
                node.category = "Math";
                break;
            case NodeType::OP_SUBTRACT:
                node.label = "Subtract";
                node.category = "Math";
                break;
            case NodeType::OP_ADD:
                node.label = "Add";
                node.category = "Math";
                break;
            case NodeType::EVENT_ON_UPDATE:
                node.label = "On Update";
                node.category = "Event";
                break;
            case NodeType::EVENT_ON_START:
                node.label = "On Start";
                node.category = "Event";
                break;
            case NodeType::PRINT:
                node.label = "Print";
                node.category = "Debug";
                break;
            default:
                node.label = "Node";
                node.category = "Unknown";
                break;
        }

        nodes[node.id] = node;
        LOGI("Added node id=%d type=%d at (%.0f,%.0f)", node.id, (int)type, x, y);
        return node.id;
    }

    bool connect(int fromId, const std::string& fromPin,
                 int toId,   const std::string& toPin) {
        if (nodes.find(fromId) == nodes.end()) return false;
        if (nodes.find(toId)   == nodes.end()) return false;
        nodes[toId].inputConnections[toPin] = {fromId, fromPin};
        LOGI("Connected node %d.%s → %d.%s", fromId, fromPin.c_str(), toId, toPin.c_str());
        return true;
    }

    void setFloat(int nodeId, const std::string& pin, float value) {
        if (nodes.find(nodeId) != nodes.end())
            nodes[nodeId].floatValues[pin] = value;
    }

    // ─── تنفيذ Graph ───────────────────────────────────────────────────────

    float evaluate(int nodeId, const std::string& outputPin,
                   std::map<std::string, float>& vars) {
        if (nodes.find(nodeId) == nodes.end()) return 0.0f;
        ScriptNode& node = nodes[nodeId];

        switch (node.type) {
            case NodeType::FLOAT_VALUE:
                return node.floatValues.count("value") ? node.floatValues["value"] : 0.0f;

            case NodeType::MATH_ABS: {
                float f = getInput(node, "F", vars);
                return std::abs(f);
            }

            case NodeType::MATH_MIN: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return std::min(a, b);
            }

            case NodeType::MATH_MAX: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return std::max(a, b);
            }

            case NodeType::MATH_SQRT: {
                float f = getInput(node, "F", vars);
                return std::sqrt(f);
            }

            case NodeType::MATH_SIN: {
                float f = getInput(node, "F", vars);
                return std::sin(f * M_PI / 180.0f);
            }

            case NodeType::MATH_COS: {
                float f = getInput(node, "F", vars);
                return std::cos(f * M_PI / 180.0f);
            }

            case NodeType::OP_SUBTRACT: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return a - b;
            }

            case NodeType::OP_ADD: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return a + b;
            }

            case NodeType::OP_MULTIPLY: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return a * b;
            }

            case NodeType::OP_DIVIDE: {
                float a = getInput(node, "A", vars);
                float b = getInput(node, "B", vars);
                return (b != 0.0f) ? a / b : 0.0f;
            }

            case NodeType::GET_VARIABLE: {
                std::string varName = node.stringValues.count("name") ? node.stringValues["name"] : "";
                return vars.count(varName) ? vars[varName] : 0.0f;
            }

            default:
                return 0.0f;
        }
    }

    // تنفيذ event chain
    std::string executeEvent(const std::string& eventName,
                              std::map<std::string, float>& vars) {
        std::ostringstream log;
        for (auto& [id, node] : nodes) {
            if ((eventName == "OnUpdate" && node.type == NodeType::EVENT_ON_UPDATE) ||
                (eventName == "OnStart"  && node.type == NodeType::EVENT_ON_START)) {
                log << "[Event:" << eventName << " node=" << id << "]\n";
                executeChain(id, vars, log);
            }
        }
        return log.str();
    }

    std::string exportToRemiScript() {
        std::ostringstream rs;
        rs << "// Generated by REMI8 Script Graph\n";
        rs << "// تم التوليد تلقائياً من Script Graph\n\n";

        for (auto& [id, node] : nodes) {
            if (node.type == NodeType::EVENT_ON_UPDATE) {
                rs << "دالة تحديث(دت) {\n";
                rs << "    // تنفيذ سلسلة العقد\n";
                generateChainCode(id, rs, 1);
                rs << "}\n\n";
            }
            if (node.type == NodeType::EVENT_ON_START) {
                rs << "دالة بداية() {\n";
                generateChainCode(id, rs, 1);
                rs << "}\n\n";
            }
        }

        return rs.str();
    }

private:
    float getInput(ScriptNode& node, const std::string& pin,
                   std::map<std::string, float>& vars) {
        if (node.inputConnections.count(pin)) {
            auto& [fromId, fromPin] = node.inputConnections[pin];
            return evaluate(fromId, fromPin, vars);
        }
        return node.floatValues.count(pin) ? node.floatValues[pin] : 0.0f;
    }

    void executeChain(int nodeId, std::map<std::string, float>& vars,
                      std::ostringstream& log) {
        // تنفيذ العقد المتصلة بـ exec pin
        for (auto& [id, node] : nodes) {
            for (auto& [pin, conn] : node.inputConnections) {
                if (pin == "exec" && conn.first == nodeId) {
                    executeNode(id, vars, log);
                    executeChain(id, vars, log);
                }
            }
        }
    }

    void executeNode(int nodeId, std::map<std::string, float>& vars,
                     std::ostringstream& log) {
        ScriptNode& node = nodes[nodeId];
        switch (node.type) {
            case NodeType::SET_VARIABLE: {
                std::string name = node.stringValues.count("name") ? node.stringValues["name"] : "x";
                float val = getInput(node, "Value", vars);
                vars[name] = val;
                log << "Set " << name << " = " << val << "\n";
                break;
            }
            case NodeType::PRINT: {
                float val = getInput(node, "Value", vars);
                log << "Print: " << val << "\n";
                LOGI("[remiscript] Print: %.4f", val);
                break;
            }
            default:
                break;
        }
    }

    void generateChainCode(int nodeId, std::ostringstream& rs, int indent) {
        std::string pad(indent * 4, ' ');
        for (auto& [id, node] : nodes) {
            for (auto& [pin, conn] : node.inputConnections) {
                if (pin == "exec" && conn.first == nodeId) {
                    switch (node.type) {
                        case NodeType::SET_VARIABLE: {
                            std::string name = node.stringValues.count("name") ? node.stringValues["name"] : "x";
                            rs << pad << "متغير " << name << " = " << generateExpr(id, "Value") << "\n";
                            break;
                        }
                        case NodeType::PRINT:
                            rs << pad << "طباعة(" << generateExpr(id, "Value") << ")\n";
                            break;
                        default:
                            break;
                    }
                    generateChainCode(id, rs, indent);
                }
            }
        }
    }

    std::string generateExpr(int nodeId, const std::string& inputPin) {
        ScriptNode& node = nodes[nodeId];
        if (!node.inputConnections.count(inputPin)) {
            if (node.floatValues.count(inputPin)) {
                std::ostringstream ss;
                ss << node.floatValues[inputPin];
                return ss.str();
            }
            return "0";
        }
        auto& [fromId, fromPin] = node.inputConnections[inputPin];
        return generateNodeExpr(fromId);
    }

    std::string generateNodeExpr(int nodeId) {
        ScriptNode& node = nodes[nodeId];
        switch (node.type) {
            case NodeType::FLOAT_VALUE: {
                std::ostringstream ss;
                ss << (node.floatValues.count("value") ? node.floatValues["value"] : 0.0f);
                return ss.str();
            }
            case NodeType::MATH_ABS:
                return "رياضيات.مطلق(" + generateExpr(nodeId, "F") + ")";
            case NodeType::MATH_MIN:
                return "رياضيات.أدنى(" + generateExpr(nodeId, "A") + "، " + generateExpr(nodeId, "B") + ")";
            case NodeType::MATH_MAX:
                return "رياضيات.أقصى(" + generateExpr(nodeId, "A") + "، " + generateExpr(nodeId, "B") + ")";
            case NodeType::MATH_SQRT:
                return "رياضيات.جذر(" + generateExpr(nodeId, "F") + ")";
            case NodeType::OP_SUBTRACT:
                return "(" + generateExpr(nodeId, "A") + " - " + generateExpr(nodeId, "B") + ")";
            case NodeType::OP_ADD:
                return "(" + generateExpr(nodeId, "A") + " + " + generateExpr(nodeId, "B") + ")";
            case NodeType::OP_MULTIPLY:
                return "(" + generateExpr(nodeId, "A") + " * " + generateExpr(nodeId, "B") + ")";
            case NodeType::OP_DIVIDE:
                return "(" + generateExpr(nodeId, "A") + " / " + generateExpr(nodeId, "B") + ")";
            default:
                return "0";
        }
    }
};

// ─── Global instance ─────────────────────────────────────────────────────────

static std::unique_ptr<ScriptGraph> gGraph;

static ScriptGraph& getGraph() {
    if (!gGraph) gGraph = std::make_unique<ScriptGraph>();
    return *gGraph;
}

// ─── JNI Functions ───────────────────────────────────────────────────────────

extern "C" {

JNIEXPORT void JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeInit(JNIEnv* env, jclass) {
    gGraph = std::make_unique<ScriptGraph>();
    LOGI("Script Graph Engine initialized (C++)");
}

JNIEXPORT jint JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeAddNode(
        JNIEnv* env, jclass, jint type, jfloat x, jfloat y) {
    return getGraph().addNode((NodeType)type, x, y);
}

JNIEXPORT jboolean JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeConnect(
        JNIEnv* env, jclass,
        jint fromId, jstring fromPin,
        jint toId,   jstring toPin) {
    const char* fp = env->GetStringUTFChars(fromPin, nullptr);
    const char* tp = env->GetStringUTFChars(toPin,   nullptr);
    bool ok = getGraph().connect(fromId, fp, toId, tp);
    env->ReleaseStringUTFChars(fromPin, fp);
    env->ReleaseStringUTFChars(toPin,   tp);
    return ok;
}

JNIEXPORT void JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeSetFloat(
        JNIEnv* env, jclass, jint nodeId, jstring pin, jfloat value) {
    const char* p = env->GetStringUTFChars(pin, nullptr);
    getGraph().setFloat(nodeId, p, value);
    env->ReleaseStringUTFChars(pin, p);
}

JNIEXPORT jfloat JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeEvaluate(
        JNIEnv* env, jclass, jint nodeId, jstring outputPin) {
    const char* p = env->GetStringUTFChars(outputPin, nullptr);
    std::map<std::string, float> vars;
    float result = getGraph().evaluate(nodeId, p, vars);
    env->ReleaseStringUTFChars(outputPin, p);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeExecuteEvent(
        JNIEnv* env, jclass, jstring eventName) {
    const char* ev = env->GetStringUTFChars(eventName, nullptr);
    std::map<std::string, float> vars;
    std::string result = getGraph().executeEvent(ev, vars);
    env->ReleaseStringUTFChars(eventName, ev);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeExportRemiScript(
        JNIEnv* env, jclass) {
    std::string code = getGraph().exportToRemiScript();
    return env->NewStringUTF(code.c_str());
}

JNIEXPORT void JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeClear(JNIEnv*, jclass) {
    getGraph().nodes.clear();
    getGraph().nextId = 1;
    LOGI("Graph cleared");
}

JNIEXPORT jstring JNICALL
Java_com_remi8_scriptgraph_ScriptGraphEngine_nativeGetGraphJson(JNIEnv* env, jclass) {
    std::ostringstream json;
    json << "{\"nodes\":[";
    bool first = true;
    for (auto& [id, node] : getGraph().nodes) {
        if (!first) json << ",";
        first = false;
        json << "{";
        json << "\"id\":" << node.id << ",";
        json << "\"type\":" << (int)node.type << ",";
        json << "\"label\":\"" << node.label << "\",";
        json << "\"category\":\"" << node.category << "\",";
        json << "\"x\":" << node.x << ",";
        json << "\"y\":" << node.y << ",";
        json << "\"w\":" << node.width << ",";
        json << "\"h\":" << node.height;
        // float values
        if (!node.floatValues.empty()) {
            json << ",\"floats\":{";
            bool ff = true;
            for (auto& [k, v] : node.floatValues) {
                if (!ff) json << ",";
                ff = false;
                json << "\"" << k << "\":" << v;
            }
            json << "}";
        }
        // connections
        if (!node.inputConnections.empty()) {
            json << ",\"inputs\":{";
            bool fi = true;
            for (auto& [pin, conn] : node.inputConnections) {
                if (!fi) json << ",";
                fi = false;
                json << "\"" << pin << "\":{\"from\":" << conn.first
                     << ",\"pin\":\"" << conn.second << "\"}";
            }
            json << "}";
        }
        json << "}";
    }
    json << "]}";
    return env->NewStringUTF(json.str().c_str());
}

} // extern "C"
