package nhb.mario3.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nhb.common.data.PuObject;
import nhb.common.data.PuObjectRO;
import nhb.common.data.exception.InvalidDataException;
import nhb.common.db.cassandra.CassandraDatasourceConfig;
import nhb.common.db.mongodb.config.MongoDBConfig;
import nhb.common.db.mongodb.config.MongoDBCredentialConfig;
import nhb.common.db.mongodb.config.MongoDBReadPreferenceConfig;
import nhb.common.db.sql.SQLDataSourceConfig;
import nhb.common.exception.UnsupportedTypeException;
import nhb.common.utils.FileSystemUtils;
import nhb.common.vo.HostAndPort;
import nhb.common.vo.UserNameAndPassword;
import nhb.mario3.config.CassandraConfig;
import nhb.mario3.config.HazelcastConfig;
import nhb.mario3.config.HttpMessageProducerConfig;
import nhb.mario3.config.KafkaMessageProducerConfig;
import nhb.mario3.config.LifeCycleConfig;
import nhb.mario3.config.ManagedObjectConfig;
import nhb.mario3.config.MessageHandlerConfig;
import nhb.mario3.config.MessageProducerConfig;
import nhb.mario3.config.MonitorAgentConfig;
import nhb.mario3.config.RabbitMQProducerConfig;
import nhb.mario3.config.RedisConfig;
import nhb.mario3.config.WorkerPoolConfig;
import nhb.mario3.config.ZkClientConfig;
import nhb.mario3.config.gateway.GatewayConfig;
import nhb.mario3.config.gateway.GatewayType;
import nhb.mario3.config.gateway.HttpGatewayConfig;
import nhb.mario3.config.gateway.KafkaGatewayConfig;
import nhb.mario3.config.gateway.RabbitMQGatewayConfig;
import nhb.mario3.config.gateway.SocketGatewayConfig;
import nhb.mario3.config.serverwrapper.HttpServerWrapperConfig;
import nhb.mario3.config.serverwrapper.RabbitMQServerWrapperConfig;
import nhb.mario3.config.serverwrapper.ServerWrapperConfig;
import nhb.mario3.config.serverwrapper.ServerWrapperConfig.ServerWrapperType;
import nhb.mario3.extension.xml.CredentialReader;
import nhb.mario3.extension.xml.EndpointReader;
import nhb.mario3.gateway.http.JettyHttpServerOptions;
import nhb.mario3.gateway.socket.SocketProtocol;
import nhb.messaging.MessagingModel;
import nhb.messaging.http.HttpMethod;
import nhb.messaging.rabbit.RabbitMQQueueConfig;

class ExtensionConfigReader extends XmlConfigReader {

	private String extensionName;

	private List<LifeCycleConfig> lifeCycleConfigs;
	private List<GatewayConfig> gatewayConfigs;
	private List<SQLDataSourceConfig> sqlDatasourceConfigs;
	private List<HazelcastConfig> hazelcastConfigs;
	private List<RedisConfig> redisConfigs;
	private List<MongoDBConfig> mongoDBConfigs;
	private List<ServerWrapperConfig> serverWrapperConfigs;
	private List<MonitorAgentConfig> monitorAgentConfigs;
	private List<MessageProducerConfig> producerConfigs;

	private final Map<String, PuObjectRO> properties = new HashMap<>();

	private List<ZkClientConfig> zkClientConfigs;

	private Collection<CassandraConfig> cassandraConfigs;

	private final PuObjectRO globalProperties;

	public ExtensionConfigReader(PuObjectRO globalProperties) {
		this.globalProperties = globalProperties;
	}

	@Override
	protected void read(Document document) throws Exception {
		System.out.println("\t\t\t- Reading extension name");
		this.extensionName = ((Node) xPath.compile("/mario/name").evaluate(document, XPathConstants.NODE))
				.getTextContent();

		if (extensionName == null || extensionName.trim().length() == 0) {
			throw new RuntimeException("extension cannot be empty");
		}

		try {
			System.out.println("\t\t\t- Reading properties");
			this.readProperties((Node) xPath.compile("/mario/properties").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading datasources config");
			this.readDataSourceConfigs(
					(Node) xPath.compile("/mario/datasources").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading server wrapper config");
			this.readServerWrapperConfigs(
					(Node) xPath.compile("/mario/servers").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading gateway configs");
			this.readGatewayConfigs((Node) xPath.compile("/mario/gateways").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading lifecycle configs");
			this.readLifeCycleConfigs(
					(Node) xPath.compile("/mario/lifecycles").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading monitor agent configs");
			this.readMonitorAgentConfigs(
					(Node) xPath.compile("/mario/monitor").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading producer configs");
			this.readProducerConfigs((Node) xPath.compile("/mario/producers").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		try {
			System.out.println("\t\t\t- Reading zookeeper configs");
			this.readCooperationsConfigs(
					(Node) xPath.compile("/mario/cooperations").evaluate(document, XPathConstants.NODE));
		} catch (Exception ex) {
			if (!(ex instanceof TransformerException) && !(ex instanceof XPathExpressionException)) {
				getLogger().error("Error", ex);
			}
		}

		System.out.println("\t\t\t- *** Reading configs done ***");
	}

	private String extractNodeContent(Node node) {
		switch (node.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
			return node.getNodeValue();
		case Node.ELEMENT_NODE:
			return node.getTextContent().trim();
		default:
			getLogger().warn("Invalid node type for read content, extension: {}", this.getExtensionName());
			break;
		}
		return null;
	}

	private InputStream readFile(final String inputFile) throws FileNotFoundException {
		String filePath = inputFile;
		if (!(filePath.startsWith(File.separator) || filePath.charAt(1) == ':')) {
			filePath = FileSystemUtils.createAbsolutePathFrom(System.getProperty("application.extensionsFolder"),
					this.getExtensionName(), filePath);
		}
		return new FileInputStream(filePath);
	}

	private void readProperties(Node properties) {
		if (properties == null) {
			return;
		}
		Node entry = properties.getFirstChild();
		while (entry != null) {
			if (entry.getNodeType() == Node.ELEMENT_NODE) {
				if (entry.getNodeName().equalsIgnoreCase("entry")) {

					Node nameAttr = entry.getAttributes().getNamedItem("name");
					Node refAttr = entry.getAttributes().getNamedItem("ref");
					Node fileAttr = entry.getAttributes().getNamedItem("file");

					String name = nameAttr == null ? null : nameAttr.getNodeValue().trim();
					String ref = refAttr == null ? null : refAttr.getNodeValue().trim();
					String file = fileAttr == null ? null : fileAttr.getNodeValue().trim();
					PuObject contentData = null;

					Node node = entry.getFirstChild();
					while (node != null) {
						if (node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.ATTRIBUTE_NODE) {
							String nodeName = node.getNodeName().toLowerCase();
							switch (nodeName) {
							case "name":
								name = extractNodeContent(node);
								break;
							case "ref":
								ref = extractNodeContent(node);
								break;
							case "file":
								file = extractNodeContent(node);
								break;
							case "variables":
								if (node.getNodeType() != Node.ELEMENT_NODE) {
									getLogger().warn("Variables node must be ELEMENT_NODE, extension: "
											+ this.getExtensionName());
								} else {
									contentData = PuObject.fromXML(node);
								}
								break;
							default:
								getLogger().warn("Unrecoginzed property's config name: " + nodeName + ", extension: "
										+ this.getExtensionName());
								break;
							}
						}
						node = node.getNextSibling();
					}

					PuObjectRO fileData = null;
					PuObjectRO refData = null;

					if (name == null) {
						throw new NullPointerException(
								"Property name cannot be null, extension: " + this.getExtensionName());
					}

					if (ref != null) {
						refData = this.getRefProperty(ref);
					}

					if (file != null) {
						try (InputStream is = readFile(file); StringWriter sw = new StringWriter()) {
							IOUtils.copy(is, sw);
							fileData = PuObject.fromXML(sw.toString());
						} catch (Exception e) {
							throw new RuntimeException("Exception while loading property from file, extension: "
									+ this.getExtensionName() + ", filePath: " + file);
						}
					}

					if (refData == null && fileData == null && contentData == null) {
						throw new InvalidDataException(
								"All 3 file data (config by 'file' attribute), content data (config by 'variables' tag) and refData are null");
					}

					PuObject data = new PuObject();
					data.addAll(refData);
					data.addAll(fileData);
					data.addAll(contentData);

					this.properties.put(name, data);
				} else {
					getLogger().warn("Invalid node name, expected for 'entry', extension: " + this.extensionName);
				}
			} else {
				getLogger().warn("Invalid node type, expected for ELEMENT_NODE, extension: " + this.extensionName);
			}
			entry = entry.getNextSibling();
		}
	}

	private ZkClientConfig readZkClientConfig(Node node) {
		if (node == null) {
			throw new NullPointerException("ZkClientConfig xml node cannot be null");
		}
		ZkClientConfig config = new ZkClientConfig();
		Node refAttr = node.getAttributes().getNamedItem("ref");
		if (refAttr != null) {
			String ref = refAttr.getNodeValue();
			PuObjectRO refObj = this.getRefProperty(ref);
			if (ref != null) {
				config.readPuObject(refObj);
			}
		}
		Node ele = node.getFirstChild();
		while (ele != null) {
			if (ele.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = ele.getNodeName();
				String nodeValue = ele.getTextContent().trim();
				switch (nodeName.toLowerCase()) {
				case "name":
					config.setName(nodeValue);
					break;
				case "servers":
					config.setServers(nodeValue);
					break;
				case "sessiontimeout":
					config.setSessionTimeout(Integer.valueOf(nodeValue));
					break;
				case "connectiontimeout":
					config.setConnectionTimeout(Integer.valueOf(nodeValue));
					break;
				case "serializerclass":
					config.setSerializerClass(nodeValue);
					break;
				case "operationretrytimeout":
					config.setOperationRetryTimeout(Long.valueOf(nodeValue));
					break;
				default:
					getLogger().warn("ZkClientConfig field name {} with value '{}' can't be recognized", nodeName,
							nodeValue);
					break;
				}
			}
			ele = ele.getNextSibling();
		}
		config.setExtensionName(this.extensionName);
		return config;
	}

	private void readCooperationsConfigs(Node node) {
		this.zkClientConfigs = new ArrayList<>();
		if (node == null) {
			return;
		}
		Node item = node.getFirstChild();
		while (item != null) {
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = item.getNodeName();
				switch (nodeName.toLowerCase()) {
				case "zookeeper":
					this.zkClientConfigs.add(readZkClientConfig(item));
					break;
				default:
					getLogger().warn("Cooperation type {} doesn't supported, Extension {}", nodeName,
							this.extensionName);
					break;
				}
			}
			item = item.getNextSibling();
		}
	}

	@SuppressWarnings("unchecked")
	private void readServerWrapperConfigs(Node node) throws XPathExpressionException {
		this.serverWrapperConfigs = new ArrayList<>();
		if (node == null) {
			return;
		}
		Node item = node.getFirstChild();
		while (item != null) {
			if (item.getNodeType() == 1) {
				ServerWrapperType connectionType = ServerWrapperType.fromName(item.getNodeName());
				switch (connectionType) {
				case HTTP: {
					HttpServerWrapperConfig httpServerWrapperConfig = new HttpServerWrapperConfig();

					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							httpServerWrapperConfig.readPuObject(refObj);
						}
					}

					Node curr = item.getFirstChild();
					while (curr != null) {
						if (curr.getNodeType() == 1) {
							switch (curr.getNodeName().trim().toLowerCase()) {
							case "name":
								httpServerWrapperConfig.setName(curr.getTextContent());
								break;
							case "port":
								httpServerWrapperConfig.setPort(Integer.valueOf(curr.getTextContent().trim()));
								break;
							case "options":
								httpServerWrapperConfig.setOptions(
										JettyHttpServerOptions.fromName(curr.getTextContent().trim()).getCode());
								break;
							case "sessiontimeout":
								httpServerWrapperConfig
										.setSessionTimeout(Integer.valueOf(curr.getTextContent().trim()));
								break;
							case "threadpool":
								readHttpThreadPoolConfig(curr, httpServerWrapperConfig);
								break;
							}
						}
						curr = curr.getNextSibling();
					}
					this.serverWrapperConfigs.add(httpServerWrapperConfig);
					break;
				}
				case RABBITMQ: {
					RabbitMQServerWrapperConfig rabbitMQServerWrapperConfig = new RabbitMQServerWrapperConfig();
					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							rabbitMQServerWrapperConfig.readPuObject(refObj);
						}
					}
					Node curr = item.getFirstChild();
					while (curr != null) {
						if (curr.getNodeType() == 1) {
							String nodeName = curr.getNodeName().trim().toLowerCase();
							switch (nodeName) {
							case "endpoint":
								System.out.println("\t\t\t\t- Reading endpoint info");
								Object endpoint = EndpointReader.read(curr);
								if (endpoint instanceof HostAndPort) {
									rabbitMQServerWrapperConfig.addEndpoint((HostAndPort) endpoint);
								} else if (endpoint instanceof Collection) {
									rabbitMQServerWrapperConfig.addEndpoints((Collection<HostAndPort>) endpoint);
								}
								break;
							case "credential":
								System.out.println("\t\t\t\t- Reading credential info");
								Object credential = CredentialReader.read(curr);
								if (credential instanceof UserNameAndPassword) {
									rabbitMQServerWrapperConfig.setCredential((UserNameAndPassword) credential);
								}
								break;
							case "name":
								System.out.println("\t\t\t\t- Reading name info");
								rabbitMQServerWrapperConfig.setName(curr.getTextContent().trim());
								break;
							case "autoreconnect":
								System.out.println("\t\t\t\t- Reading autoreconnect info");
								getLogger().warn("Autoreconnect is default and cannot be set, it's deprecated");
								break;
							default:
								System.out
										.println("\t\t\t\t- !!! ERROR !!! --> invalid tag name: " + curr.getNodeName());
								// throw new RuntimeException("invalid tag name:
								// " + curr.getNodeName());
							}
						}
						curr = curr.getNextSibling();
					}

					this.serverWrapperConfigs.add(rabbitMQServerWrapperConfig);
					break;
				}
				default:
					getLogger().warn("Connection type not supported: " + connectionType);
				}
			}
			item = item.getNextSibling();
		}
		for (ServerWrapperConfig config : this.serverWrapperConfigs) {
			config.setExtensionName(this.extensionName);
		}
	}

	private void readHttpThreadPoolConfig(Node node, HttpServerWrapperConfig httpServerWrapperConfig) {
		Node curr = node.getFirstChild();
		while (curr != null) {
			String nodeName = curr.getNodeName().toLowerCase();
			switch (nodeName) {
			case "minsize": {
				String nodeValue = curr.getTextContent();
				httpServerWrapperConfig.setMinAcceptorThreadPoolSize(Integer.valueOf(nodeValue));
				break;
			}
			case "maxsize": {
				String nodeValue = curr.getTextContent();
				httpServerWrapperConfig.setMaxAcceptorThreadPoolSize(Integer.valueOf(nodeValue));
				break;
			}
			case "taskqueue": {
				Node taskCurrNode = curr.getFirstChild();
				while (taskCurrNode != null) {
					String taskQueueConfName = taskCurrNode.getNodeName().toLowerCase();
					switch (taskQueueConfName) {
					case "initsize": {
						int initSize = Integer.valueOf(taskCurrNode.getTextContent().trim());
						httpServerWrapperConfig.setTaskQueueInitSize(initSize);
						break;
					}
					case "growby": {
						int growBy = Integer.valueOf(taskCurrNode.getTextContent().trim());
						httpServerWrapperConfig.setTaskQueueGrowBy(growBy);
						break;
					}
					case "maxsize": {
						int maxSize = Integer.valueOf(taskCurrNode.getTextContent().trim());
						httpServerWrapperConfig.setTaskQueueMaxSize(maxSize);
						break;
					}
					}
				}
				break;
			}
			}
			curr = curr.getNextSibling();
		}
	}

	private WorkerPoolConfig readWorkerPoolConfig(Node node) throws XPathExpressionException {
		WorkerPoolConfig workerPoolConfig = null;
		if (node != null) {
			workerPoolConfig = new WorkerPoolConfig();

			Node refAttr = node.getAttributes().getNamedItem("ref");
			if (refAttr != null) {
				String ref = refAttr.getNodeValue();
				PuObjectRO refObj = this.getRefProperty(ref);
				if (ref != null) {
					workerPoolConfig.readPuObject(refObj);
				}
			}
			Node element = node.getFirstChild();
			while (element != null) {
				if (element.getNodeType() == 1) {
					String value = element.getTextContent().trim();
					String nodeName = element.getNodeName();
					if (nodeName.equalsIgnoreCase("poolsize")) {
						workerPoolConfig.setPoolSize(Integer.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("ringbuffersize")) {
						workerPoolConfig.setRingBufferSize(Integer.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("threadnamepattern")) {
						workerPoolConfig.setThreadNamePattern(value);
					}
				}
				element = element.getNextSibling();
			}
		}
		return workerPoolConfig;
	}

	private RabbitMQQueueConfig readRabbitMQQueueConfig(Node node) {
		RabbitMQQueueConfig queueConfig = null;
		if (node != null) {
			queueConfig = new RabbitMQQueueConfig();
			Node element = node.getFirstChild();
			while (element != null) {
				if (element.getNodeType() == 1) {
					String nodeName = element.getNodeName();
					String value = element.getTextContent().trim();
					if (nodeName.equalsIgnoreCase("name") || nodeName.equalsIgnoreCase("queuename")) {
						queueConfig.setQueueName(value);
					} else if (nodeName.equalsIgnoreCase("autoack")) {
						queueConfig.setAutoAck(Boolean.valueOf(element.getTextContent()));
					} else if (nodeName.equalsIgnoreCase("exchangename")) {
						queueConfig.setExchangeName(value);
					} else if (nodeName.equalsIgnoreCase("exchangetype")) {
						queueConfig.setExchangeType(value);
					} else if (nodeName.equalsIgnoreCase("routingkey")) {
						queueConfig.setRoutingKey(value);
					} else if (nodeName.equalsIgnoreCase("type") || nodeName.equalsIgnoreCase("messagingmodel")) {
						queueConfig.setType(MessagingModel.fromName(value));
					} else if (nodeName.equalsIgnoreCase("qos")) {
						queueConfig.setQos(Integer.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("durable")) {
						queueConfig.setDurable(Boolean.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("exclusive")) {
						queueConfig.setExclusive(Boolean.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("autoDelete")) {
						queueConfig.setAutoDelete(Boolean.valueOf(value));
					} else if (nodeName.equalsIgnoreCase("variables") || nodeName.equalsIgnoreCase("arguments")) {
						queueConfig.setArguments(PuObject.fromXML(element).toMap());
					}
				}
				element = element.getNextSibling();
			}
		}
		return queueConfig;
	}

	private void readGatewayConfigs(Node node) throws XPathExpressionException {
		this.gatewayConfigs = new ArrayList<GatewayConfig>();
		NodeList list = (NodeList) xPath.compile("*").evaluate(node, XPathConstants.NODESET);
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			GatewayType type = GatewayType.fromName(item.getNodeName());
			if (type != null) {
				GatewayConfig config = null;
				Node ele = null;
				switch (type) {
				case KAFKA: {
					KafkaGatewayConfig kafkaGatewayConfig = new KafkaGatewayConfig();

					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							kafkaGatewayConfig.readPuObject(refObj);
						}
					}

					ele = item.getFirstChild();
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String value = ele.getTextContent().trim();
							String nodeName = ele.getNodeName();
							if (nodeName.equalsIgnoreCase("name")) {
								kafkaGatewayConfig.setName(value);
							} else if (nodeName.equalsIgnoreCase("serializer")) {
								kafkaGatewayConfig.setSerializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("deserializer")) {
								kafkaGatewayConfig.setDeserializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("workerpool")) {
								kafkaGatewayConfig.setWorkerPoolConfig(readWorkerPoolConfig(ele));
							} else if (nodeName.equalsIgnoreCase("config") || nodeName.equalsIgnoreCase("configuration")
									|| nodeName.equalsIgnoreCase("configFile")
									|| nodeName.equalsIgnoreCase("configurationFile")) {
								kafkaGatewayConfig.setConfigFile(value);
							} else if (nodeName.equalsIgnoreCase("topics")) {
								String[] arr = value.split(",");
								for (String str : arr) {
									str = str.trim();
									if (str.length() > 0) {
										kafkaGatewayConfig.getTopics().add(str);
									}
								}
							} else if (nodeName.equalsIgnoreCase("pollTimeout")) {
								kafkaGatewayConfig.setPollTimeout(Integer.valueOf(value));
							}
						}
						ele = ele.getNextSibling();
					}
					config = kafkaGatewayConfig;
					break;
				}
				case HTTP: {
					HttpGatewayConfig httpGatewayConfig = new HttpGatewayConfig();
					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							httpGatewayConfig.readPuObject(refObj);
						}
					}
					ele = item.getFirstChild();
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String value = ele.getTextContent().trim();
							String nodeName = ele.getNodeName();
							if (nodeName.equalsIgnoreCase("deserializer")) {
								httpGatewayConfig.setDeserializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("serializer")) {
								httpGatewayConfig.setSerializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("name")) {
								httpGatewayConfig.setName(value);
							} else if (nodeName.equalsIgnoreCase("workerpool")) {
								httpGatewayConfig.setWorkerPoolConfig(readWorkerPoolConfig(ele));
							} else if (nodeName.equalsIgnoreCase("path") || nodeName.equalsIgnoreCase("location")) {
								httpGatewayConfig.setPath(value);
							} else if (nodeName.equalsIgnoreCase("async")) {
								httpGatewayConfig.setAsync(Boolean.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("encoding")) {
								httpGatewayConfig.setEncoding(value);
							} else if (nodeName.equalsIgnoreCase("server")) {
								httpGatewayConfig.setServerWrapperName(value);
							} else if (nodeName.equalsIgnoreCase("usemultipart")
									|| nodeName.equalsIgnoreCase("usingmultipart")) {
								httpGatewayConfig.setUseMultipath(Boolean.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("header")) {
								String key = ele.getAttributes().getNamedItem("name").getNodeValue();
								if (key != null && key.trim().length() > 0) {
									httpGatewayConfig.getHeaders().put(key.trim(), value);
								}
							}
						}
						ele = ele.getNextSibling();
					}
					config = httpGatewayConfig;
					break;
				}
				case RABBITMQ: {
					RabbitMQGatewayConfig rabbitMQConfig = new RabbitMQGatewayConfig();
					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							rabbitMQConfig.readPuObject(refObj);
						}
					}
					ele = item.getFirstChild();
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String value = ele.getTextContent().trim();
							if (ele.getNodeName().equalsIgnoreCase("deserializer")) {
								rabbitMQConfig.setDeserializerClassName(value);
							} else if (ele.getNodeName().equalsIgnoreCase("serializer")) {
								rabbitMQConfig.setSerializerClassName(value);
							} else if (ele.getNodeName().equalsIgnoreCase("name")) {
								rabbitMQConfig.setName(value);
							} else if (ele.getNodeName().equalsIgnoreCase("workerpool")) {
								rabbitMQConfig.setWorkerPoolConfig(readWorkerPoolConfig(ele));
							} else if (ele.getNodeName().equalsIgnoreCase("server")) {
								rabbitMQConfig.setServerWrapperName(value);
							} else if (ele.getNodeName().equalsIgnoreCase("queue")) {
								rabbitMQConfig.setQueueConfig(readRabbitMQQueueConfig(ele));
							}
						}
						ele = ele.getNextSibling();
					}
					config = rabbitMQConfig;
					break;
				}
				case SOCKET: {
					SocketGatewayConfig socketGatewayConfig = new SocketGatewayConfig();
					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							socketGatewayConfig.readPuObject(refObj);
						}
					}
					ele = item.getFirstChild();
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String nodeName = ele.getNodeName();
							String value = ele.getTextContent().trim();
							if (nodeName.equalsIgnoreCase("protocol")) {
								socketGatewayConfig.setProtocol(SocketProtocol.fromName(value));
							} else if (nodeName.equalsIgnoreCase("host")) {
								socketGatewayConfig.setHost(value);
							} else if (nodeName.equalsIgnoreCase("port")) {
								socketGatewayConfig.setPort(Integer.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("deserializer")) {
								socketGatewayConfig.setDeserializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("serializer")) {
								socketGatewayConfig.setSerializerClassName(value);
							} else if (nodeName.equalsIgnoreCase("name")) {
								socketGatewayConfig.setName(value);
							} else if (nodeName.equalsIgnoreCase("workerpool")) {
								socketGatewayConfig.setWorkerPoolConfig(readWorkerPoolConfig(ele));
							} else if (nodeName.equalsIgnoreCase("uselengthprepender")
									|| nodeName.equalsIgnoreCase("usinglengthprepender")
									|| nodeName.equalsIgnoreCase("prependlength")) {
								socketGatewayConfig.setUseLengthPrepender(Boolean.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("bootGroupThreads")) {
								socketGatewayConfig.setBootEventLoopGroupThreads(Integer.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("workerGroupThreads")) {
								socketGatewayConfig.setWorkerEventLoopGroupThreads(Integer.valueOf(value));
							}
						}
						ele = ele.getNextSibling();
					}
					config = socketGatewayConfig;
					break;
				}
				default:
					throw new RuntimeException(type + " gateway doesn't supported now");
				}

				if (config != null) {
					config.setExtensionName(this.extensionName);
					gatewayConfigs.add(config);
				}
			} else {
				getLogger().warn("gateway type not found: {}", item.getNodeName());
			}
		}
	}

	private PuObject readPuObjectFromNode(Node node) {
		PuObject puo = new PuObject();
		if (node.getAttributes().getNamedItem("ref") != null) {
			String reference = node.getAttributes().getNamedItem("ref").getNodeValue().trim();
			if (this.properties.containsKey(reference)) {
				puo.addAll(this.properties.get(reference).deepClone());
			} else if (this.globalProperties.variableExists(reference)) {
				puo.addAll(this.globalProperties.getPuObject(reference).deepClone());
			} else {
				throw new NullPointerException(
						"Property not found for name: " + reference + ", extension: " + this.getExtensionName());
			}
		}
		puo.addAll(PuObject.fromXML(node));
		return puo;
	}

	private PuObjectRO getRefProperty(String name) {
		if (this.properties.containsKey(name)) {
			return this.properties.get(name);
		} else if (this.globalProperties.variableExists(name)) {
			return this.globalProperties.getPuObject(name);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void readDataSourceConfigs(Node node) throws XPathExpressionException {
		if (this.sqlDatasourceConfigs == null) {
			this.sqlDatasourceConfigs = new ArrayList<SQLDataSourceConfig>();
		}

		NodeList list = (NodeList) xPath.compile("*").evaluate(node, XPathConstants.NODESET);
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item.getNodeName().equalsIgnoreCase("sql")) {

				SQLDataSourceConfig config = new SQLDataSourceConfig();

				Node refAttr = item.getAttributes().getNamedItem("ref");
				if (refAttr != null) {
					String ref = refAttr.getNodeValue();
					PuObjectRO refObj = this.getRefProperty(ref);
					if (ref != null) {
						config.readPuObject(refObj);
					}
				}

				Node ele = item.getFirstChild();
				while (ele != null) {
					if (ele.getNodeType() == Node.ELEMENT_NODE) {
						String nodeName = ele.getNodeName().toLowerCase();
						switch (nodeName) {
						case "name": {
							config.setName(ele.getTextContent().trim());
							break;
						}
						case "properties":
						case "propertiesFile": {
							try (InputStream is = readFile(ele.getTextContent().trim())) {
								Properties props = new Properties();
								props.load(is);
								config.setProperties(props);
							} catch (Exception e) {
								throw new RuntimeException(
										"Read file error, SQL config at extension name: " + this.getExtensionName());
							}
							break;
						}
						case "variables": {
							config.setProperties(PuObject.fromXML(ele));
							break;
						}
						}
					}
					ele = ele.getNextSibling();
				}

				this.sqlDatasourceConfigs.add(config);
			} else if (item.getNodeName().equalsIgnoreCase("cassandra")) {
				CassandraConfig config = new CassandraConfig();
				Node refAttr = item.getAttributes().getNamedItem("ref");
				if (refAttr != null) {
					String ref = refAttr.getNodeValue();
					PuObjectRO refObj = this.getRefProperty(ref);
					if (ref != null) {
						config.readPuObject(refObj);
					}
				}
				Node currNode = item.getFirstChild();
				while (currNode != null) {
					if (currNode.getNodeType() == 1) {
						if (currNode.getNodeName().equalsIgnoreCase("name")) {
							config.setName(currNode.getTextContent().trim());
						} else if (currNode.getNodeName().equalsIgnoreCase("endpoint")) {
							Object obj = EndpointReader.read(currNode);
							if (obj instanceof HostAndPort) {
								config.getEndpoints().add((HostAndPort) obj);
							} else if (obj instanceof Collection<?>) {
								config.getEndpoints().addAll((Collection<? extends HostAndPort>) obj);
							}
						} else if (currNode.getNodeName().equalsIgnoreCase("keyspace")) {
							config.setKeyspace(currNode.getTextContent().trim());
						}
					}
					currNode = currNode.getNextSibling();
				}
				if (this.cassandraConfigs == null) {
					this.cassandraConfigs = new HashSet<>();
				}
				this.cassandraConfigs.add(config);
			} else if (item.getNodeName().equalsIgnoreCase("hazelcast")) {
				HazelcastConfig config = new HazelcastConfig();
				Node refAttr = item.getAttributes().getNamedItem("ref");
				if (refAttr != null) {
					String ref = refAttr.getNodeValue();
					PuObjectRO refObj = this.getRefProperty(ref);
					if (ref != null) {
						config.readPuObject(refObj);
					}
				}
				Node curr = item.getFirstChild();
				while (curr != null) {
					if (curr.getNodeType() == 1) {
						String value = curr.getTextContent().trim();
						switch (curr.getNodeName().trim().toLowerCase()) {
						case "name":
							config.setName(value);
							break;
						case "config":
						case "configfile":
							config.setConfigFilePath(value);
							break;
						case "member":
						case "ismember":
							config.setMember(Boolean.valueOf(value));
							break;
						case "initializer":
						case "initializerClass":
							getLogger().warn(
									"the initializer class config is now DEPRECATED, please use 'initializers' to specific lifecycle names");
							config.setInitializerClass(value);
							break;
						case "lazyinit":
						case "islazyinit":
							config.setLazyInit(Boolean.valueOf(value));
							break;
						case "autoinit":
						case "autoinitonextensionready":
							config.setAutoInitOnExtensionReady(Boolean.valueOf(value));
							break;
						case "initializers":
							Node entryNode = curr.getFirstChild();
							while (entryNode != null) {
								if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
									config.getInitializers().add(entryNode.getTextContent().trim());
								}
								entryNode = entryNode.getNextSibling();
							}
							break;
						default:
							break;
						}
					}
					curr = curr.getNextSibling();
				}
				config.setExtensionName(extensionName);
				if (this.hazelcastConfigs == null) {
					this.hazelcastConfigs = new ArrayList<>();
				}
				this.hazelcastConfigs.add(config);
			} else if (item.getNodeName().equalsIgnoreCase("redis")) {
				RedisConfig config = new RedisConfig();
				Node refAttr = item.getAttributes().getNamedItem("ref");
				if (refAttr != null) {
					String ref = refAttr.getNodeValue();
					PuObjectRO refObj = this.getRefProperty(ref);
					if (ref != null) {
						config.readPuObject(refObj);
					}
				}
				config.setName(((Node) xPath.compile("name").evaluate(item, XPathConstants.NODE)).getTextContent());
				try {
					config.setRedisType(
							((Node) xPath.compile("type").evaluate(item, XPathConstants.NODE)).getTextContent());
				} catch (Exception ex) {
					// default is false
				}
				try {
					config.setMasterName(
							((Node) xPath.compile("mastername").evaluate(item, XPathConstants.NODE)).getTextContent());
				} catch (Exception ex) {
					// do nothing
				}
				NodeList endpoints = (NodeList) xPath.compile("endpoint/entry").evaluate(item, XPathConstants.NODESET);
				for (int j = 0; j < endpoints.getLength(); j++) {
					Node endpointNode = endpoints.item(j);
					String host = null;
					int port = -1;
					boolean isMaster = false;
					try {
						host = ((Node) xPath.compile("host").evaluate(endpointNode, XPathConstants.NODE))
								.getTextContent();
					} catch (Exception ex) {
						getLogger().warn("host config is invalid : " + endpointNode.getTextContent(), ex);
					}
					try {
						port = Integer
								.valueOf(((Node) xPath.compile("port").evaluate(endpointNode, XPathConstants.NODE))
										.getTextContent());
					} catch (Exception ex) {
						getLogger().warn("port config is invalid : " + endpointNode.getTextContent(), ex);
					}
					try {
						isMaster = Boolean
								.valueOf(((Node) xPath.compile("master").evaluate(endpointNode, XPathConstants.NODE))
										.getTextContent());
					} catch (Exception ex) {
						// getLogger().warn("master config is invalid : " +
						// endpointNode.getTextContent(), ex);
					}
					if (host != null && port > 0) {
						HostAndPort endpoint = new HostAndPort(host, port);
						endpoint.setMaster(isMaster);
						config.addEndpoint(endpoint);
					}
				}
				try {
					config.setTimeout(Integer.valueOf(
							((Node) xPath.compile("timeout").evaluate(item, XPathConstants.NODE)).getTextContent()));
				} catch (Exception ex) {
				}
				try {
					config.setPoolSize(Integer.valueOf(
							((Node) xPath.compile("poolsize").evaluate(item, XPathConstants.NODE)).getTextContent()));
				} catch (Exception ex) {
				}
				config.setExtensionName(extensionName);
				if (this.redisConfigs == null) {
					this.redisConfigs = new ArrayList<>();
				}
				this.redisConfigs.add(config);
			} else if (item.getNodeName().equalsIgnoreCase("mongodb")) {
				MongoDBConfig config = new MongoDBConfig();

				Node refAttr = item.getAttributes().getNamedItem("ref");
				if (refAttr != null) {
					String ref = refAttr.getNodeValue();
					PuObjectRO refObj = this.getRefProperty(ref);
					if (ref != null) {
						config.readPuObject(refObj);
					}
				}

				Node currNode = item.getFirstChild();
				while (currNode != null) {
					String nodeName = currNode.getNodeName().toLowerCase();
					if (currNode.getNodeType() == 1) {
						switch (nodeName) {
						case "name":
							config.setName(currNode.getTextContent().trim());
							break;
						case "endpoint":
						case "endpoints":
							Object endpoint = null;
							try {
								endpoint = EndpointReader.read(currNode);
							} catch (RuntimeException e) {
								getLogger().error("Invalid endpoint config for mongoDB, extension: {}",
										this.extensionName);
								throw e;
							}
							if (endpoint != null) {
								if (endpoint instanceof HostAndPort) {
									config.addEndpoint((HostAndPort) endpoint);
								} else if (endpoint instanceof Collection) {
									for (HostAndPort hnp : (Collection<HostAndPort>) endpoint) {
										config.addEndpoint(hnp);
									}
								}
							}
							break;
						case "credential":
						case "credentials": {
							Node credentialEntry = currNode.getFirstChild();
							while (credentialEntry != null) {
								if (credentialEntry.getNodeType() == Node.ELEMENT_NODE) {
									String credentialNodeName = credentialEntry.getNodeName().toLowerCase();
									if (credentialNodeName.equalsIgnoreCase("userName")
											|| credentialNodeName.equalsIgnoreCase("password")
											|| credentialNodeName.equalsIgnoreCase("authDB")) {
										// read as single credential
										config.addCredentialConfig(new MongoDBCredentialConfig(credentialEntry));
									} else if (credentialNodeName.equalsIgnoreCase("entry")) {
										// read as multi credential config
										while (credentialEntry != null) {
											if (credentialEntry.getNodeType() == Node.ELEMENT_NODE) {
												credentialNodeName = credentialEntry.getNodeName().toLowerCase();
												if (credentialNodeName.equalsIgnoreCase("entry")) {
													config.addCredentialConfig(
															new MongoDBCredentialConfig(credentialEntry));
												} else {
													getLogger().warn("Invalid credential section: {}, ignored",
															credentialNodeName);
												}
											}
											credentialEntry = credentialEntry.getNextSibling();
										}
										break;
									}
								}
								credentialEntry = credentialEntry.getNextSibling();
							}
							break;
						}
						case "readpreference": {
							config.setReadPreference(new MongoDBReadPreferenceConfig(currNode));
							break;
						}
						default:
							getLogger().warn("Mongodb config section is unrecognized: {}, extension: {}", nodeName,
									this.extensionName);
							break;
						}
					}
					currNode = currNode.getNextSibling();
				}
				if (this.mongoDBConfigs == null) {
					this.mongoDBConfigs = new ArrayList<MongoDBConfig>();
				}
				this.mongoDBConfigs.add(config);
			} else {
				getLogger().warn("datasource type is not supported: " + item.getNodeName());
			}
		}
	}

	private void readLifeCycleConfigs(Node node) throws XPathExpressionException {
		// read startup config
		this.lifeCycleConfigs = new ArrayList<LifeCycleConfig>();
		if (node == null) {
			return;
		}
		Node item = node.getFirstChild();
		while (item != null) {
			if (item.getNodeType() == 1) {
				LifeCycleConfig config = null;
				if (item.getNodeName().equalsIgnoreCase("handler")) {
					MessageHandlerConfig messageHandlerConfig = new MessageHandlerConfig();
					Object gatewaysObj = xPath.compile("bind/gateway").evaluate(item, XPathConstants.NODESET);
					if (gatewaysObj != null) {
						NodeList gateways = (NodeList) gatewaysObj;
						for (int j = 0; j < gateways.getLength(); j++) {
							messageHandlerConfig.getBindingGateways().add(gateways.item(j).getTextContent().trim());
						}
					}
					config = messageHandlerConfig;
				} else if (item.getNodeName().equalsIgnoreCase("managedobject")) {
					config = new ManagedObjectConfig();
				} else if (item.getNodeName().equalsIgnoreCase("entry")) {
					config = new LifeCycleConfig();
				}

				if (config != null) {
					Node refAttr = item.getAttributes().getNamedItem("ref");
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							config.readPuObject(refObj);
						}
					}
					String name = ((Node) xPath.compile("name").evaluate(item, XPathConstants.NODE)).getTextContent();
					String handleClass = ((Node) xPath.compile("handle").evaluate(item, XPathConstants.NODE))
							.getTextContent();
					config.setName(name);
					config.setExtensionName(extensionName);
					config.setHandleClass(handleClass);

					Node variableObj = (Node) xPath.compile("variables").evaluate(item, XPathConstants.NODE);
					if (variableObj != null) {
						config.setInitParams(readPuObjectFromNode(variableObj));
					}

					this.lifeCycleConfigs.add(config);
				} else {
					getLogger().warn("lifecycle definition cannot be recognized: " + item);
				}
			}
			item = item.getNextSibling();
		}
	}

	private void readMonitorAgentConfigs(Node node) throws Exception {
		this.monitorAgentConfigs = new ArrayList<>();
	}

	private void readProducerConfigs(Node node) throws XPathExpressionException {
		this.producerConfigs = new ArrayList<>();
		if (node == null) {
			return;
		}
		Node item = node.getFirstChild();
		while (item != null) {
			if (item.getNodeType() == 1) {
				GatewayType gatewayType = GatewayType.fromName(item.getNodeName());
				Node refAttr = item.getAttributes().getNamedItem("ref");
				MessageProducerConfig config = null;
				Node ele = item.getFirstChild();
				switch (gatewayType) {
				case KAFKA: {
					KafkaMessageProducerConfig kafkaProducerConfig = new KafkaMessageProducerConfig();
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							kafkaProducerConfig.readPuObject(refObj);
						}
					}
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String nodeName = ele.getNodeName();
							String value = ele.getTextContent().trim();
							if (nodeName.equalsIgnoreCase("config") || nodeName.equalsIgnoreCase("configuration")
									|| nodeName.equalsIgnoreCase("configFile")
									|| nodeName.equalsIgnoreCase("configurationFile")) {
								kafkaProducerConfig.setConfigFile(value);
							} else if (nodeName.equalsIgnoreCase("topic")) {
								kafkaProducerConfig.setTopic(value);
							}
						}
						ele = ele.getNextSibling();
					}
					config = kafkaProducerConfig;
					break;
				}
				case RABBITMQ: {
					RabbitMQProducerConfig rabbitMQProducerConfig = new RabbitMQProducerConfig();
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							rabbitMQProducerConfig.readPuObject(refObj);
						}
					}
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String nodeName = ele.getNodeName();
							String value = ele.getTextContent().trim();
							if (nodeName.equalsIgnoreCase("server")) {
								rabbitMQProducerConfig.setConnectionName(value);
							} else if (nodeName.equalsIgnoreCase("timeout")) {
								rabbitMQProducerConfig.setTimeout(Integer.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("queue")) {
								rabbitMQProducerConfig.setQueueConfig(readRabbitMQQueueConfig(ele));
							}
						}
						ele = ele.getNextSibling();
					}
					config = rabbitMQProducerConfig;
					break;
				}
				case HTTP: {
					HttpMessageProducerConfig httpMessageProducerConfig = new HttpMessageProducerConfig();
					if (refAttr != null) {
						String ref = refAttr.getNodeValue();
						PuObjectRO refObj = this.getRefProperty(ref);
						if (ref != null) {
							httpMessageProducerConfig.readPuObject(refObj);
						}
					}
					while (ele != null) {
						if (ele.getNodeType() == 1) {
							String nodeName = ele.getNodeName();
							String value = ele.getTextContent().trim();
							if (nodeName.equalsIgnoreCase("endpoint")) {
								httpMessageProducerConfig.setEndpoint(value);
							} else if (nodeName.equalsIgnoreCase("method")) {
								httpMessageProducerConfig.setHttpMethod(HttpMethod.fromName(value));
							} else if (nodeName.equalsIgnoreCase("async")) {
								httpMessageProducerConfig.setAsync(Boolean.valueOf(value));
							} else if (nodeName.equalsIgnoreCase("usemultipart")
									|| nodeName.equalsIgnoreCase("usingmultipart")) {
								httpMessageProducerConfig.setUsingMultipart(Boolean.valueOf(value));
							}
						}
						ele = ele.getNextSibling();
					}
					config = httpMessageProducerConfig;
					break;
				}
				case SOCKET:
				default:
					throw new UnsupportedTypeException();
				}

				if (config != null) {
					if (config.getGatewayType() == null) {
						config.setGatewayType(gatewayType);
					}
					config.setName(((Node) xPath.compile("name").evaluate(item, XPathConstants.NODE)).getTextContent());
					config.setExtensionName(this.extensionName);
					this.producerConfigs.add(config);
				}
			}
			item = item.getNextSibling();
		}
	}

	public String getExtensionName() {
		return extensionName;
	}

	public List<GatewayConfig> getGatewayConfigs() {
		return this.gatewayConfigs;
	}

	public List<SQLDataSourceConfig> getSQLDataSourceConfig() {
		return this.sqlDatasourceConfigs;
	}

	public List<HazelcastConfig> getHazelcastConfigs() {
		return hazelcastConfigs;
	}

	public List<RedisConfig> getRedisConfigs() {
		return this.redisConfigs;
	}

	public List<LifeCycleConfig> getLifeCycleConfigs() {
		return this.lifeCycleConfigs;
	}

	public List<MongoDBConfig> getMongoDBConfigs() {
		return this.mongoDBConfigs;
	}

	public List<ServerWrapperConfig> getServerWrapperConfigs() {
		return this.serverWrapperConfigs;
	}

	public Collection<? extends MonitorAgentConfig> getMonitorAgentConfigs() {
		return this.monitorAgentConfigs;
	}

	public Collection<? extends MessageProducerConfig> getProducerConfigs() {
		return this.producerConfigs;
	}

	public Collection<? extends CassandraDatasourceConfig> getCassandraConfigs() {
		return this.cassandraConfigs;
	}

	public Collection<? extends ZkClientConfig> getZkClientConfigs() {
		return this.zkClientConfigs;
	}

	public PuObjectRO getProperty(String name) {
		return this.properties.get(name);
	}
}
