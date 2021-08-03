package com.example.SimbirSoft_2021.service;

import com.example.SimbirSoft_2021.Dto.ProjectDto;
import com.example.SimbirSoft_2021.Dto.RoleDto;
import com.example.SimbirSoft_2021.Dto.TaskDto;
import com.example.SimbirSoft_2021.entity.ProjectEntity;
import com.example.SimbirSoft_2021.entity.ReleaseEntity;
import com.example.SimbirSoft_2021.entity.TaskEntity;
import com.example.SimbirSoft_2021.exception.*;
import com.example.SimbirSoft_2021.mappers.ProjectMapper;
import com.example.SimbirSoft_2021.mappers.RoleMapper;
import com.example.SimbirSoft_2021.mappers.TaskMapper;
import com.example.SimbirSoft_2021.repository.ProjectCrud;
import com.example.SimbirSoft_2021.repository.ReleaseCrud;
import com.example.SimbirSoft_2021.repository.RoleCrud;
import com.example.SimbirSoft_2021.repository.TaskCrud;
import com.example.SimbirSoft_2021.service.interfaceService.StandartServiceInterface;
import com.example.SimbirSoft_2021.service.interfaceService.TaskServiceInterface;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 1 способ
//@RequiredArgsConstructor
@Service
public class TaskService implements StandartServiceInterface<TaskDto>, TaskServiceInterface {

    // 2 способ
    //@Autowired
    //private TaskCrud projectCRUD;

    private final TaskCrud taskCrud; // создаём интерфейс для взаимодействия с бд
    private final ReleaseCrud releaseCrud;
    private final ProjectCrud projectCrud;
    private final RoleService roleService;

    // 3 способ
    public TaskService(TaskCrud taskCrud, ReleaseCrud releaseCrud, ProjectCrud projectCrud, RoleService roleService) {
        this.taskCrud = taskCrud;
        this.releaseCrud = releaseCrud;
        this.projectCrud = projectCrud;
        this.roleService = roleService;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- регистрация
    public TaskDto registration(TaskDto taskDto) throws ReleaseExistsException, ReleaseNotFoundException, TaskAndDateTimeExistsException,
            ProjectAndDateTimeExistsException, TaskExistsException {

        //  проверка
        if (releaseCrud.findByReleaseId(taskDto.getReleaseId())==null){ // проверить, что есть такая реализация существует
            throw new ReleaseNotFoundException();
        }
        if (taskCrud.findByReleaseId(taskDto.getReleaseId())!=null){ // что реализации нет среди задач
            throw new TaskAndDateTimeExistsException();
        }
        if (projectCrud.findByReleaseId(taskDto.getReleaseId())!=null){ // что реализации нет среди проектов
            throw new ProjectAndDateTimeExistsException();
        }
        if (taskCrud.findByTaskNameAndProjectIdAndReleaseId(taskDto.getTaskName(), taskDto.getProjectId(), taskDto.getReleaseId())!=null){ // что нет похожей задачи у проекта
            throw new TaskExistsException();
        }

        TaskEntity taskEntity = TaskMapper.INSTANCE.toEntity(taskDto);

        // сохраняем
        taskCrud.save(taskEntity);
        return TaskMapper.INSTANCE.toDto(taskEntity);
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- вытащить все задачи
    public List<TaskDto> getAll() throws TaskNotFoundException {
        List<TaskEntity> taskEntityList = taskCrud.findAll();

        //  проверка на то что задачи вообще существуют
        if (taskEntityList==null){
            throw new TaskNotFoundException();
        }

        // перевод коллекции из одного вида в другой
        List<TaskDto> taskDtoList = taskEntityList.stream().map(x-> TaskMapper.INSTANCE.toDto(x)).collect(Collectors.toList());

        return taskDtoList;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- вытащить одну задачу
    public TaskDto getOne(Long id) throws TaskNotFoundException {
        TaskEntity taskEntity = taskCrud.findByTaskId(id);

        //  проверка на то что задачи вообще существуют
        if (taskEntity==null){
            throw new TaskNotFoundException();
        }

        return TaskMapper.INSTANCE.toDto(taskEntity);
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // --------------- поиск по реализации
    public TaskDto findByReleaseId(Long releaseId) {
        TaskEntity taskEntity = taskCrud.findByReleaseId(releaseId);

        //  проверка на то что задачи вообще существуют
        if (taskEntity==null){
            return null;
        }

        return TaskMapper.INSTANCE.toDto(taskEntity);
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- удалить одну задачу
    public Long deleteOne(Long id) throws TaskNotFoundException, ReleaseNotFoundException, RoleNotFoundException {
        TaskEntity taskEntity = taskCrud.findByTaskId(id);

        //  проверка на то что задача вообще существуют
        if (taskEntity==null){
            throw new TaskNotFoundException();
        }

        ReleaseEntity releaseEntity = releaseCrud.findByReleaseId(taskEntity.getReleaseId());
        if (releaseEntity!=null){
            releaseCrud.delete(releaseEntity);
        }

        roleService.deleteByTaskId(id);
        taskCrud.delete(taskEntity);
        return id;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- удалить задачи связанные с проектом
    public boolean deleteTaskByProjectId(Long projectId) throws TaskNotFoundException, ReleaseNotFoundException, RoleNotFoundException {
        List<TaskEntity> taskEntityList = taskCrud.findAll();

        //  проверка на то что задача вообще существуют
        if (taskEntityList==null){
            throw new TaskNotFoundException();
        }

        //  вытаскиваем и удаляем по одной задачи, и сохраняем в коллекцию
        for (TaskEntity e:taskEntityList){
            if (e.getProjectId() == projectId){ //  проверка
                deleteOne(e.getTaskId());
            }
        }

        return true;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- удалить реализацию связанную с задачей
    public boolean deleteReleaseInTask(Long id) {
        TaskEntity taskEntity = taskCrud.findByReleaseId(id);
        if(taskEntity!=null){
            taskEntity.setReleaseId(null);
            taskCrud.save(taskEntity);
        }
        return true;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    @Override // ----------------- обновить одну задачу
    public TaskDto updateOne(Long id, TaskDto taskDto) throws TaskNotFoundException, ReleaseExistsException, ReleaseNotFoundException,
            TaskAndDateTimeExistsException, ProjectAndDateTimeExistsException, TaskExistsException {

        //  проверка на то что задача вообще существуют
        if (taskCrud.findByTaskId(id)==null){
            throw new TaskNotFoundException();
        }

        TaskEntity taskEntityNew = TaskMapper.INSTANCE.toEntity(taskDto);
        TaskEntity taskEntity = taskCrud.findByTaskId(id);

        //  проверка
        if (releaseCrud.findByReleaseId(taskEntityNew.getReleaseId())==null){ // проверить, что есть такая реализация существует
            throw new ReleaseNotFoundException();
        }
        if ((taskCrud.findByReleaseId(taskEntityNew.getReleaseId())!=null)
                &&(taskEntity.getReleaseId()!=taskEntityNew.getReleaseId())){ // что реализации нет среди задач
            throw new TaskAndDateTimeExistsException();
        }
        if (projectCrud.findByReleaseId(taskEntityNew.getReleaseId())!=null){ // что реализации нет среди проектов
            throw new ProjectAndDateTimeExistsException();
        }
        TaskEntity taskEntityTest = taskCrud.findByTaskNameAndProjectIdAndReleaseId(taskEntityNew.getTaskName(), taskEntityNew.getProjectId(), taskEntityNew.getReleaseId());
        if ((taskEntityTest!=null)&&(taskEntityTest!=taskEntity)){ // что нет похожей задачи у проекта
            throw new TaskExistsException();
        }

        // присваивание новых значений
        taskEntity.setTaskName(taskEntityNew.getTaskName());
        taskEntity.setTaskStatus(taskEntityNew.getTaskStatus());
        taskEntity.setProjectId(taskEntityNew.getProjectId());
        taskEntity.setReleaseId(taskEntityNew.getReleaseId());

        // сохранение
        taskCrud.save(taskEntity);
        return TaskMapper.INSTANCE.toDto(taskEntity);
    }

}
