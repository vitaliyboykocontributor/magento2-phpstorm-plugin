<?php

namespace Test\TestModule\Model\ResourceModel;

use Magento\Framework\Model\ResourceModel\Db\AbstractDb;

/**
 * AccessLog Resource Model
 */
class AccessLog extends AbstractDb
{
    /**
     * Define main table
     *
     * @return void
     */
    protected function _construct()
    {
        $this->_init('test_accesslog', 'entity_id');
    }
}