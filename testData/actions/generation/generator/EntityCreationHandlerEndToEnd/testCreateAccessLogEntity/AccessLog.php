<?php

namespace Test\TestModule\Model;

use Test\TestModule\Model\ResourceModel\AccessLog as AccessLogResource;
use Magento\Framework\Model\AbstractModel;

/**
 * AccessLog Model
 */
class AccessLog extends AbstractModel
{
    /**
     * Cache tag
     */
    const CACHE_TAG = 'test_testmodule_accesslog';

    /**
     * Cache tag
     *
     * @var string
     */
    protected $_cacheTag = self::CACHE_TAG;

    /**
     * Event prefix
     *
     * @var string
     */
    protected $_eventPrefix = 'test_testmodule_accesslog';

    /**
     * Event object
     *
     * @var string
     */
    protected $_eventObject = 'accesslog';

    /**
     * Initialize resource model
     *
     * @return void
     */
    protected function _construct()
    {
        $this->_init(AccessLogResource::class);
    }

    /**
     * Get Entity ID
     *
     * @return int|null
     */
    public function getEntityId()
    {
        return $this->getData('entity_id');
    }

    /**
     * Set Entity ID
     *
     * @param int $entityId
     * @return $this
     */
    public function setEntityId($entityId)
    {
        return $this->setData('entity_id', $entityId);
    }

    /**
     * Get Number Of Calls
     *
     * @return string|null
     */
    public function getNumberOfCalls()
    {
        return $this->getData('number_of_calls');
    }

    /**
     * Set Number Of Calls
     *
     * @param string $numberOfCalls
     * @return $this
     */
    public function setNumberOfCalls($numberOfCalls)
    {
        return $this->setData('number_of_calls', $numberOfCalls);
    }

    /**
     * Get Route
     *
     * @return string|null
     */
    public function getRoute()
    {
        return $this->getData('route');
    }

    /**
     * Set Route
     *
     * @param string $route
     * @return $this
     */
    public function setRoute($route)
    {
        return $this->setData('route', $route);
    }
}