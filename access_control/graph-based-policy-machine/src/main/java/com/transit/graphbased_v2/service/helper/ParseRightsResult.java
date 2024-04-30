package com.transit.graphbased_v2.service.helper;

import com.transit.graphbased_v2.domain.graph.nodes.ClazzType;
import com.transit.graphbased_v2.domain.graph.nodes.ObjectAttributeExtendedClazz;
import com.transit.graphbased_v2.performacelogging.LogExecutionTime;
import com.transit.graphbased_v2.repository.ObjectAttributeClazzRepository;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.transit.graphbased_v2.service.helper.StringListHelper.listToSetString;
import static org.neo4j.driver.internal.value.NullValue.NULL;

@Component
public class ParseRightsResult {

    @Autowired
    private ObjectAttributeClazzRepository objectAttributeClazzRepository;

    @LogExecutionTime
    public List<ObjectAttributeExtendedClazz> parseResult(Result result, boolean mergeAll, boolean mergeByOid) {
        List<ObjectAttributeExtendedClazz> results = new ArrayList<>();
        Set<UUID> objectIds = new HashSet<>();
        while (result.hasNext()) {
            var nextRecord = result.next();
            var r = parseResult(nextRecord, true);
            if (results.isEmpty()) {
                results.add(r);
                objectIds.add(r.getOId());
            } else if (mergeAll) {
                var temp = results.get(0);
                overwriteProperties(r, temp);
                results.remove(0);
                results.add(temp);
                objectIds.add(r.getOId());
            } else if (mergeByOid) {
                if (objectIds.contains(r.getOId())) {
                    var temp = results.stream().filter(e -> e.getOId().equals(r.getOId())).findFirst().get();
                    overwriteProperties(r, temp);
                    results.replaceAll(e -> {
                        if (e.getOId().equals(temp.getOId())) {
                            return temp;
                        }
                        return e;
                    });

                } else {
                    results.add(r);
                    objectIds.add(r.getOId());
                }
            } else {
                results.add(r);
            }
        }
        return results;
    }

    @LogExecutionTime
    public ObjectAttributeExtendedClazz parseResult(Record record, boolean dataFromRecord) {
        ObjectAttributeExtendedClazz oa = new ObjectAttributeExtendedClazz();
        var temp = record.get(0);

        // get values from record
        if (dataFromRecord) {
            String idString = temp.get("id").asString();
            UUID id = UUID.fromString(idString);
            String name = temp.get("name").asString();
            String entityClass = temp.get("type").asString();


            List<Object> readProperties = temp.get("readProperties").asList();
            List<Object> writeProperties = temp.get("writeProperties").asList();
            List<Object> shareReadProperties = temp.get("shareReadProperties").asList();
            List<Object> shareWriteProperties = temp.get("shareWriteProperties").asList();

            oa.setReadProperties(listToSetString(readProperties));
            oa.setWriteProperties(listToSetString(writeProperties));
            oa.setShareReadProperties(listToSetString(shareReadProperties));
            oa.setShareWriteProperties(listToSetString(shareWriteProperties));


            oa.setId(id);
            oa.setName(name);
            oa.setType(ClazzType.valueOf(entityClass));

            oa.setOId(UUID.fromString(record.get(1).asString()));
            oa.setOEntityClazz(record.get(2).asString());


        } else {

            var oaNormal = objectAttributeClazzRepository.findById(UUID.fromString(temp.get("id").asString())).get();
            oa.setId(oaNormal.getId());
            oa.setName(oaNormal.getName());
            oa.setType(oaNormal.getType());

            oa.setReadProperties(oaNormal.getReadProperties());
            oa.setWriteProperties(oaNormal.getWriteProperties());
            oa.setShareWriteProperties(oaNormal.getShareReadProperties());
            oa.setShareWriteProperties(oaNormal.getShareWriteProperties());

            oa.setOId(UUID.fromString(record.get(1).asString()));
        }

        return oa;
    }

    private void overwriteProperties(ObjectAttributeExtendedClazz r, ObjectAttributeExtendedClazz temp) {

        if (r.getId() != null && temp.getId() == null) {
            temp.setId(r.getId());
        }

        var readPropertiesTemp = temp.getReadProperties();
        readPropertiesTemp.addAll(r.getReadProperties());
        temp.setReadProperties(readPropertiesTemp);

        var writePropertiesTemp = temp.getWriteProperties();
        writePropertiesTemp.addAll(r.getWriteProperties());
        temp.setWriteProperties(writePropertiesTemp);

        var shareReadPropertiesTemp = temp.getShareReadProperties();
        shareReadPropertiesTemp.addAll(r.getShareReadProperties());
        temp.setShareReadProperties(shareReadPropertiesTemp);

        var shareWritePropertiesTemp = temp.getShareWriteProperties();
        shareWritePropertiesTemp.addAll(r.getShareWriteProperties());
        temp.setShareWriteProperties(shareWritePropertiesTemp);

    }


    @LogExecutionTime
    public List<ObjectAttributeExtendedClazz> parseCheckResult(Result result, String[] keys, boolean mergeAll) {
        List<ObjectAttributeExtendedClazz> results = new ArrayList<>();

        List<Record> records = result.list();
        if (mergeAll) {
            int i = 0;
            for (Record record : records) {
                for (String key : keys) {
                    Value temp = record.get(key);
                    var c = parseCheckResult(temp, key);
                    if (i >= 1) {
                        for (ObjectAttributeExtendedClazz item : results) {
                            if (item.getName() == key) {
                                if ("OA".equals(c.getType().toString())) {
                                    overwriteProperties(c, item);
                                }
                            }
                        }
                    } else {
                        results.add(c);
                    }

                }
                i++;
            }
        } else {
            // Check if there are any records
            if (!records.isEmpty()) {
                // Get the first record
                Record record = records.get(0);

                // Process each key in the provided keys array for the first record
                for (String key : keys) {
                    Value temp = record.get(key);
                    results.add(parseCheckResult(temp, key));
                }
            }
        }


        return results;
    }

    @LogExecutionTime
    public ObjectAttributeExtendedClazz parseCheckResult(Value temp, String nodeName) {
        ObjectAttributeExtendedClazz node = new ObjectAttributeExtendedClazz();

        if (temp == NULL) {
            String name = nodeName;
            String entityClass = "OA";
            node.setOEntityClazz("empty");
            node.setName(name);
            node.setType(ClazzType.valueOf(entityClass));
            return node;
        }


        String idString = temp.get("id").asString();
        UUID id = UUID.fromString(idString);
        String name = temp.get("name").asString();
        String entityClass = temp.get("type").asString();

        //just for error fix
        if ("UA".equals(entityClass) || nodeName.equals("i")) {
            entityClass = "I";
        }


        node.setId(id);
        node.setName(name);
        node.setType(ClazzType.valueOf(entityClass));

        if ("OA".equals(entityClass)) {
            node.setName(nodeName);

            List<Object> readProperties = temp.get("readProperties").asList();
            List<Object> writeProperties = temp.get("writeProperties").asList();
            List<Object> shareReadProperties = temp.get("shareReadProperties").asList();
            List<Object> shareWriteProperties = temp.get("shareWriteProperties").asList();

            node.setReadProperties(listToSetString(readProperties));
            node.setWriteProperties(listToSetString(writeProperties));
            node.setShareReadProperties(listToSetString(shareReadProperties));
            node.setShareWriteProperties(listToSetString(shareWriteProperties));
        }


        return node;
    }

    @LogExecutionTime
    public ObjectAttributeExtendedClazz mergeObjectAttributes(ObjectAttributeExtendedClazz oa, ObjectAttributeExtendedClazz oa2) {

        var prop = oa.getReadProperties();
        prop.addAll(oa2.getReadProperties());
        oa.setReadProperties(prop);

        prop = oa.getWriteProperties();
        prop.addAll(oa2.getWriteProperties());
        oa.setWriteProperties(prop);

        prop = oa.getShareReadProperties();
        prop.addAll(oa2.getShareReadProperties());
        oa.setShareReadProperties(prop);

        prop = oa.getShareWriteProperties();
        prop.addAll(oa2.getShareWriteProperties());
        oa.setShareWriteProperties(prop);


        return oa;
    }


}

