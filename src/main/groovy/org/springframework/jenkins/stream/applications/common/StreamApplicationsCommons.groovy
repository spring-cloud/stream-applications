package org.springframework.jenkins.stream.applications.common

import groovy.transform.CompileStatic

/**
 * @author Soby Chacko
 */
@CompileStatic
class StreamApplicationsCommons {

    public static final Map<String, String> PHASED_JOBS = [
            //sources
            'cdc-debezium-source'               : 'source/cdc-debezium-source',
            'file-source'                       : 'source/file-source',
            'ftp-source'                        : 'source/ftp-source',
            'geode-source'                      : 'source/geode-source',
            'http-source'                       : 'source/http-source',
            'jdbc-source'                       : 'source/jdbc-source',
            'jms-source'                        : 'source/jms-source',
            'load-generator-source'             : 'source/load-generator-source',
            'mail-source'                       : 'source/mail-source',
            'mongodb-source'                    : 'source/mongodb-source',
            'mqtt-source'                       : 'source/mqtt-source',
            'rabbit-source'                     : 'source/rabbit-source',
            's3-source'                         : 'source/s3-source',
            'sftp-source'                       : 'source/sftp-source',
            'syslog-source'                     : 'source/syslog-source',
            'tcp-source'                        : 'source/tcp-source',
            'time-source'                       : 'source/time-source',
            'twitter-message-source'            : 'source/twitter-message-source',
            'twitter-search-source'             : 'source/twitter-search-source',
            'twitter-stream-source'             : 'source/twitter-stream-source',
            'websocket-source'                  : 'source/websocket-source',
            'zeromq-source'                     : 'source/zeromq-source',
            //sinks
            'cassandra-sink'                    : 'sink/cassandra-sink',
            'analytics-sink'                    : 'sink/analytics-sink',
            'elasticsearch-sink'                : 'sink/elasticsearch-sink',
            'file-sink'                         : 'sink/file-sink',
            'ftp-sink'                          : 'sink/ftp-sink',
            'geode-sink'                        : 'sink/geode-sink',
            'jdbc-sink'                         : 'sink/jdbc-sink',
            'log-sink'                          : 'sink/log-sink',
            'mongodb-sink'                      : 'sink/mongodb-sink',
            'mqtt-sink'                         : 'sink/mqtt-sink',
            'pgcopy-sink'                       : 'sink/pgcopy-sink',
            'rabbit-sink'                       : 'sink/rabbit-sink',
            'redis-sink'                        : 'sink/redis-sink',
            'router-sink'                       : 'sink/router-sink',
            'rsocket-sink'                      : 'sink/rsocket-sink',
            's3-sink'                           : 'sink/s3-sink',
            'sftp-sink'                         : 'sink/sftp-sink',
            'tcp-sink'                          : 'sink/tcp-sink',
            'throughput-sink'                   : 'sink/throughput-sink',
            'twitter-message-sink'              : 'sink/twitter-message-sink',
            'twitter-update-sink'               : 'sink/twitter-update-sink',
            'wavefront-sink'                    : 'sink/wavefront-sink',
            'websocket-sink'                    : 'sink/websocket-sink',
            'zeromq-sink'                       : 'sink/zeromq-sink',
            //processors
            'aggregator-processor'              : 'processor/aggregator-processor',
            'bridge-processor'                  : 'processor/bridge-processor',
            'filter-processor'                  : 'processor/filter-processor',
            'groovy-processor'                  : 'processor/groovy-processor',
            'header-enricher-processor'         : 'processor/header-enricher-processor',
            'http-request-processor'            : 'processor/http-request-processor',
            'image-recognition-processor'       : 'processor/image-recognition-processor',
            'object-detection-processor'        : 'processor/object-detection-processor',
            'semantic-segmentation-processor'   : 'processor/semantic-segmentation-processor',
            'script-processor'                  : 'processor/script-processor',
            'splitter-processor'                : 'processor/splitter-processor',
            'transform-processor'               : 'processor/transform-processor',
            'twitter-trend-processor'           : 'processor/twitter-trend-processor',
    ]

    public static final Map<String, String> PHASED_JOBS_2020 = [
            //sources
            'cdc-debezium-source'               : 'source/cdc-debezium-source',
            'file-source'                       : 'source/file-source',
            'ftp-source'                        : 'source/ftp-source',
            'geode-source'                      : 'source/geode-source',
            'http-source'                       : 'source/http-source',
            'jdbc-source'                       : 'source/jdbc-source',
            'jms-source'                        : 'source/jms-source',
            'load-generator-source'             : 'source/load-generator-source',
            'mail-source'                       : 'source/mail-source',
            'mongodb-source'                    : 'source/mongodb-source',
            'mqtt-source'                       : 'source/mqtt-source',
            'rabbit-source'                     : 'source/rabbit-source',
            's3-source'                         : 'source/s3-source',
            'sftp-source'                       : 'source/sftp-source',
            'syslog-source'                     : 'source/syslog-source',
            'tcp-source'                        : 'source/tcp-source',
            'time-source'                       : 'source/time-source',
            'twitter-message-source'            : 'source/twitter-message-source',
            'twitter-search-source'             : 'source/twitter-search-source',
            'twitter-stream-source'             : 'source/twitter-stream-source',
            'websocket-source'                  : 'source/websocket-source',
            //sinks
            'cassandra-sink'                    : 'sink/cassandra-sink',
            'analytics-sink'                    : 'sink/analytics-sink',
            'elasticsearch-sink'                : 'sink/elasticsearch-sink',
            'file-sink'                         : 'sink/file-sink',
            'ftp-sink'                          : 'sink/ftp-sink',
            'geode-sink'                        : 'sink/geode-sink',
            'jdbc-sink'                         : 'sink/jdbc-sink',
            'log-sink'                          : 'sink/log-sink',
            'mongodb-sink'                      : 'sink/mongodb-sink',
            'mqtt-sink'                         : 'sink/mqtt-sink',
            'pgcopy-sink'                       : 'sink/pgcopy-sink',
            'rabbit-sink'                       : 'sink/rabbit-sink',
            'redis-sink'                        : 'sink/redis-sink',
            'router-sink'                       : 'sink/router-sink',
            'rsocket-sink'                      : 'sink/rsocket-sink',
            's3-sink'                           : 'sink/s3-sink',
            'sftp-sink'                         : 'sink/sftp-sink',
            'tcp-sink'                          : 'sink/tcp-sink',
            'tasklauncher-sink'                 : 'sink/tasklauncher-sink',
            'throughput-sink'                   : 'sink/throughput-sink',
            'twitter-message-sink'              : 'sink/twitter-message-sink',
            'twitter-update-sink'               : 'sink/twitter-update-sink',
            'wavefront-sink'                    : 'sink/wavefront-sink',
            'websocket-sink'                    : 'sink/websocket-sink',
            //processors
            'aggregator-processor'              : 'processor/aggregator-processor',
            'bridge-processor'                  : 'processor/bridge-processor',
            'filter-processor'                  : 'processor/filter-processor',
            'groovy-processor'                  : 'processor/groovy-processor',
            'header-enricher-processor'         : 'processor/header-enricher-processor',
            'http-request-processor'            : 'processor/http-request-processor',
            'image-recognition-processor'       : 'processor/image-recognition-processor',
            'object-detection-processor'        : 'processor/object-detection-processor',
            'semantic-segmentation-processor'   : 'processor/semantic-segmentation-processor',
            'script-processor'                  : 'processor/script-processor',
            'splitter-processor'                : 'processor/splitter-processor',
            'transform-processor'               : 'processor/transform-processor',
            'twitter-trend-processor'           : 'processor/twitter-trend-processor',
    ]

    public static final List<String> PHASE1_KEYS = ['file-source', 'ftp-source', 'geode-source', 'http-source',
                                                    'jdbc-source', 'jms-source','load-generator-source', 'mail-source',
                                                    'mongodb-source', 'mqtt-source']

    public static final List<String> PHASE2_KEYS = ['rabbit-source', 'syslog-source', 's3-source', 'sftp-source', 'tcp-source',
                                                    'time-source', 'twitter-message-source', 'twitter-search-source',
                                                    'twitter-stream-source', 'websocket-source']

    public static final List<String> PHASE3_KEYS = ['cdc-debezium-source', 'zeromq-source', 'cassandra-sink', 'analytics-sink', 'elasticsearch-sink',
                                                    'file-sink', 'ftp-sink', 'geode-sink', 'jdbc-sink', 'log-sink']

    public static final List<String> PHASE3_KEYS_2020 = ['cdc-debezium-source', 'cassandra-sink', 'analytics-sink', 'elasticsearch-sink',
                                                    'file-sink', 'ftp-sink', 'geode-sink', 'jdbc-sink', 'log-sink']


    public static final List<String> PHASE4_KEYS = ['mongodb-sink', 'mqtt-sink', 'pgcopy-sink', 'rabbit-sink', 'redis-sink', 'router-sink',
                                                    'rsocket-sink', 's3-sink', 'sftp-sink', 'tcp-sink']

    public static final List<String> PHASE5_KEYS = ['throughput-sink', 'wavefront-sink', 'websocket-sink', 'twitter-message-sink', 'twitter-update-sink',
                                                    'zeromq-sink', 'bridge-processor', 'filter-processor', 'groovy-processor']

    public static final List<String> PHASE5_KEYS_2020 = ['tasklauncher-sink', 'throughput-sink', 'wavefront-sink', 'websocket-sink', 'twitter-message-sink', 'twitter-update-sink',
                                                    'bridge-processor', 'filter-processor', 'groovy-processor']

    public static final List<String> PHASE6_KEYS = ['header-enricher-processor', 'http-request-processor', 'image-recognition-processor', 'object-detection-processor',
                                                    'semantic-segmentation-processor', 'script-processor', 'splitter-processor',
                                                    'transform-processor', 'twitter-trend-processor',  'aggregator-processor']

    public static final List<List<String>> ALL_JOBS = [PHASE1_KEYS, PHASE2_KEYS, PHASE3_KEYS, PHASE4_KEYS, PHASE5_KEYS, PHASE6_KEYS]

    public static final List<List<String>> ALL_JOBS_2020 = [PHASE1_KEYS, PHASE2_KEYS, PHASE3_KEYS_2020, PHASE4_KEYS, PHASE5_KEYS_2020, PHASE6_KEYS]

}
