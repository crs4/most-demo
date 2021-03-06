# -*- coding: utf-8 -*-

#
# Project MOST - Moving Outcomes to Standard Telemedicine Practice
# http://most.crs4.it/
#
# Copyright 2014-2015, CRS4 srl. (http://www.crs4.it/)
# Dual licensed under the MIT or GPL Version 2 licenses.
# See license-GPLv2.txt or license-MIT.txt

from django.db import models
from django.utils.translation import ugettext_lazy as _

from most.web.users.models import MostUser, TaskGroup
from most.web.streaming.models import StreamingDevice
from most.web.utils import pkgen
import time
import calendar


class Device(models.Model):

    """Class Device:

    Attributes:
        uuid                   (django.db.models.CharField)    : unique identifier
    """

    APPLICATION_TYPES = (
        ('AA', 'Applicant App'),
        ('SA', 'Specialist App'),
    )

    DEVICE_PLATFORM = (
        ('IOS', 'Apple platform'),
        ('ANDROID', 'Android platform')
    )

    uuid = models.CharField(_('Device UUID'), max_length=100, unique=True)
    application_type = models.CharField(_('Application type'), choices=APPLICATION_TYPES, max_length=2)
    device_platform = models.CharField(_('Device platform'), choices=DEVICE_PLATFORM, max_length=7)
    task_groups = models.ManyToManyField(TaskGroup, related_name='devices', null=True, blank=True, verbose_name=_('Device Task Groups'))


    def __unicode__(self):
        return '%s in %s with uuid: %s' % (self.get_application_type_display(), self.get_device_platform_display(), self.uuid)

class Room(models.Model):

    uuid = models.CharField(max_length=40, unique=True, default=pkgen)
    name = models.CharField(max_length=200)
    description = models.CharField(max_length=200)
    encoder_device = models.ForeignKey(StreamingDevice, related_name="encoder_rooms", blank=True, null=True)
    camera_device = models.ForeignKey(StreamingDevice, related_name="camera_rooms", blank=True, null=True)
    task_group = models.ForeignKey(TaskGroup, related_name="rooms")
    has_encoder = models.BooleanField(default=True)
    has_camera = models.BooleanField(default=True)

    def __unicode__(self):
        return '[Room: {name} - {description} - Taskgroup: {tgname}]'.format(name=self.name, description=self.description, tgname=self.task_group.name)

    def _get_json_dict(self):

        return {
            'uuid': self.uuid,
            'name': self.name,
            'description': self.description,
        }

    json_dict = property(_get_json_dict)

    def _get_full_json_dict(self):

        devices = {}
        if self.has_camera:
            devices['camera'] = self.camera_device.json_dict
        if self.has_encoder:
            devices['encoder'] = self.encoder_device.json_dict

        return {
            'uuid': self.uuid,
            'name': self.name,
            'description': self.description,
            'task_group': self.task_group.json_dict,
            'devices': devices
        }

    full_json_dict = property(_get_full_json_dict)


class Teleconsultation(models.Model):

    TELECONSULTATION_STATE = (
        ('NEW', 'New Teleconsultation'), #Created from applicant
        ('OPEN', 'At Least one Session Open - (SESSION WAITING STATE'),
        ('ACTIVE', 'Last Session in progress'),
        ('CLOSE', 'Last Session is closed')
    )

    URGENCY_STATE = (
        ('LOW', 'Low severity'),
        ('NORMAL', 'Normal severity'),
        ('URGENCY', 'Urgency'),
        ('EMERGENCY', 'Emergency')
    )

    uuid = models.CharField(max_length=40, unique=True, default=pkgen)
    applicant = models.ForeignKey(MostUser, related_name="has_applicant", blank=True, null=True)
    specialist = models.ForeignKey(MostUser, related_name="has_specialist", blank=True, null=True)
    description = models.CharField(_('Description'), max_length=200)
    task_group = models.ForeignKey(TaskGroup, related_name="teleconsultations")
    state = models.CharField(_('Teleconsultation State'), choices=TELECONSULTATION_STATE, max_length=20)
    severity = models.CharField(_('Severity State'), choices=URGENCY_STATE, max_length=20, default="NORMAL")
    created = models.DateTimeField(auto_now_add=True)
    updated = models.DateTimeField(auto_now=True)

    def __unicode__(self):
        return '[Teleconsultation: {uuid} - {description} - Taskgroup: {tgname}]'.format(uuid=self.uuid, description=self.description, tgname=self.task_group.name)

    def _get_json_dict(self):
        
        result  = {
            'uuid': self.uuid,
            'description': self.description,
            'created': calendar.timegm(self.created.timetuple()),
            'applicant' : { 'username' : self.applicant.get_full_name() , 'voip_data' : None if len(self.applicant.account_set.all())<1  else  self.applicant.account_set.all()[0].json_dict},
            'specialist' : None if self.specialist == None else { 'username' : self.specialist.get_full_name() ,
                                         'voip_data' : None if len(self.specialist.account_set.all())<1  else self.specialist.account_set.all()[0].json_dict}
        }

        #Check sessions
        if self.sessions.count() > 0:
            last_session = self.sessions.order_by('-created')[0]
            result['last_session'] = last_session.json_dict

        return result

    json_dict = property(_get_json_dict)

    def _get_full_json_dict(self):

        return {
            'uuid': self.uuid,
            'description': self.description,
            'task_group': self.task_group.json_dict,
            'applicant' : { 'username' : self.applicant.get_full_name() , 'voip_data' : None if len(self.applicant.account_set.all())<1  else  self.applicant.account_set.all()[0].json_dict},
            'specialist' : None if self.specialist == None else { 'username' : self.specialist.get_full_name() ,
                                         'voip_data' : None if len(self.specialist.account_set.all())<1  else self.specialist.account_set.all()[0].json_dict}
        }

    full_json_dict = property(_get_full_json_dict)


class TeleconsultationSession(models.Model):

    SESSION_STATE = (
        ('NEW', 'New Session'), #Created from applicant
        ('WAITING', 'Session waiting for specialist'), #Started from applicant
        ('READY', 'Session ready to start'), #Started from applicant
        ('RUN', 'Session in progress'), #Accepted by Specialist
        ('CLOSE', 'Session is closed'), #Closed from applicant or specialist
        ('CANCELED', 'Session is canceled') #Canceled from applicant or specialist
    )

    uuid = models.CharField(max_length=40, unique=True, default=pkgen)
    teleconsultation = models.ForeignKey(Teleconsultation, related_name="sessions")
    state = models.CharField(_('Teleconsultation Session State'), choices=SESSION_STATE, max_length=20, default='NEW')
    created = models.DateTimeField(auto_now_add=True)
    updated = models.DateTimeField(auto_now=True)
    room = models.ForeignKey(Room, related_name="sessions")

    def __unicode__(self):
        return '[Teleconsultation Session: {uuid}]'.format(uuid=self.uuid)

    def _get_json_dict(self):

        return {
            'uuid': self.uuid,
            'created': calendar.timegm(self.created.timetuple()),
            'updated': calendar.timegm(self.created.timetuple()),
            'state': self.state,
           
        }

    json_dict = property(_get_json_dict)

    def _get_full_json_dict(self):

        result = self.json_dict
        result.update({
            'teleconsultation': self.teleconsultation.full_json_dict,
            'room': self.room.full_json_dict
        })
        return result

    full_json_dict = property(_get_full_json_dict)

