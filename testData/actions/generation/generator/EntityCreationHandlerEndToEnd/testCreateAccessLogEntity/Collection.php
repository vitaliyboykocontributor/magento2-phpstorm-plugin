<?php

namespace Test\TestModule\Model\ResourceModel\AccessLog;

use Test\TestModule\Model\AccessLog;
use Test\TestModule\Model\ResourceModel\AccessLog as AccessLogResource;
use Magento\Framework\Model\ResourceModel\Db\Collection\AbstractCollection;

/**
 * AccessLog Collection
 */
class Collection extends AbstractCollection
{
    /**
     * Define model & resource model
     *
     * @return void
     */
    protected function _construct()
    {
        $this->_init(AccessLog::class, AccessLogResource::class);
    }
}